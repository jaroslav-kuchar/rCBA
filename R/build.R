#' @title Build classifier function (Apriori-based)
#' @description Automatic build of the classification model using the Apriori algorithm from the \code{arules}
#'
#' @param trainData \code{data.frame} or \code{transactions} from \code{arules} with input data
#' @param className column name with the target class - default is the last column
#' @param pruning performing pruning while building the model
#' @param sa simulated annealing setting. Default values: list(temp=100.0, alpha=0.05, tabuRuleLength=5, timeout=10)
#' @param verbose verbose indicator
#' @param parallel parallel indicator
#' @return list with parameters and model as data.frame with rules
#' @export
#' @examples
#' library("rCBA")
#' data("iris")
#'
#' output <- rCBA::build(iris,sa = list(alpha=0.5), parallel=FALSE) # speeding up the cooling
#' model <- output$model
#'
#' predictions <- rCBA::classification(iris, model)
#' table(predictions)
#' sum(as.character(iris$Species)==as.character(predictions), na.rm=TRUE) / length(predictions)
#'
#' @include init.R
build <- function(trainData, className=NA, pruning=TRUE, sa=list(), verbose = TRUE, parallel=TRUE){
  if(verbose){
    message(paste(Sys.time()," rCBA: initialized",sep=""))
    start.time <- proc.time()
  }

  # convert data to frame if passed as transactions
  if(is(trainData,"transactions")){
    trainData <- transactionsToFrame(trainData)
  }

	# sa settings
	sa <- modifyList(list(temp=100.0, alpha=0.05, tabuRuleLength=5, timeout=10), sa)

	# classname
	if(!exists("className") || is.na(className)){
		className <- tail(names(trainData),1)
	}

	# preprocess
	trainData <- sapply(trainData,as.factor)
	trainData <- data.frame(trainData, check.names=F)
	# trainData <- trainData[!is.na(trainData[[className]]),]

	# create train and test fold using stratified 2fold
	folds <- generateCVRuns(labels = replace(c(trainData[[className]]), is.na(trainData[[className]]), paste("rCBA_unique-", Sys.time(), sep="")),ntimes = 1,nfold = 4,stratified=TRUE)
	testIndex <- folds[[1]][[1]]
	# testIndex <- testIndex[!is.na(testIndex)]
	# create train and test fold using random sample
	testSet <- trainData[testIndex,]
	trainSet <- trainData[-testIndex,]
	# convert the trainset to transactions
	txns <- as(trainSet, "transactions")

	if(verbose){
	  message(paste(Sys.time()," rCBA: data ",paste(dim(trainData), collapse="x"),sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}
	start.time <- proc.time()


	# initial temperature
	temp <- sa$temp
	# cooling parameter
	alpha <- sa$alpha
	# tabu rule length - max rule length
	tabuRuleLength <- sa$tabuRuleLength
	# current and best solution
	currentSolution <- c(runif(1,0,1),runif(1,0,1),round(runif(1,1,min(ncol(trainData),tabuRuleLength))))
	currentSolutionAccuracy <- .evaluate(currentSolution[1], currentSolution[2], currentSolution[3], txns, trainSet, testSet, className, pruning, sa$timeout, parallel)
	bestSolution <- currentSolution
	bestSolutionAccuracy <- currentSolutionAccuracy

	accuracies <- c(currentSolutionAccuracy)
	iteration <- 0

	if(verbose){
	  cat("Iteration: ")
	}
	# start
	while(temp>1.0){
	  if(verbose){
	    cat(paste(iteration, "-", sep = ""))
	  }
		iteration <- iteration + 1
		# generate new solution
		newSolution <- currentSolution
		# change randomly selected parameter
		parameter <- round(runif(1,1,3))
		# support or confidence
		if(parameter==1 || parameter==2){
			# if not initiated properly
			if(bestSolutionAccuracy<=0){
				newSolution[parameter] <- runif(1,0,1)
			} else {
				if(currentSolutionAccuracy==-1){
					# mining failed - increase support/confidence
					# direction <- min(newSolution[parameter]/3.0, 0.1)
					direction <- runif(1,0,1-newSolution[parameter])
				} else if(currentSolutionAccuracy==0){
					# no results yet - decrease support/confidence
					# direction <- -min(newSolution[parameter]/3.0, 0.1)
					direction <- -runif(1,0,newSolution[parameter])
				} else {
					# otherwise - random
					# direction <- runif(1,-min(newSolution[parameter]/3.0, 0.1), min(newSolution[parameter]/3.0, 0.1))
					direction <- runif(1,-newSolution[parameter], 1-newSolution[parameter])
				}
				step <- abs(direction)
				# increase or decrease if possible
				if(direction>0 && newSolution[parameter]<=(1-step)){
					newSolution[parameter] <- round(newSolution[parameter] + step, 4)
				}
				if(direction<0 && newSolution[parameter] >= (0+step)){
					newSolution[parameter] <- round(newSolution[parameter] - step, 4)
				}
			}
		}
		# rule length
		if(parameter==3){
			# if not initiated properly
			if(bestSolutionAccuracy<=0){
				newSolution[parameter] <- round(runif(1,1,min(ncol(trainData),tabuRuleLength-1)))
			} else {
				if(currentSolutionAccuracy==-1){
					# mining failed -> shorter rules
					direction <- 0.1
				} else {
					# otherwise random
					direction <- runif(1,0,1)
				}
				# change value if possible
				if(direction<0.5 && newSolution[parameter]>1){
					newSolution[parameter] <- newSolution[parameter] - 1
				}
				if(direction>0.5 && newSolution[parameter]<min(ncol(trainData),tabuRuleLength-1)){
					newSolution[parameter] <- newSolution[parameter] + 1
				}
			}
		}
		# compute accuracy
		newSolutionAccuracy <- .evaluate(newSolution[1], newSolution[2], newSolution[3], txns, trainSet, testSet, className, pruning, sa$timeout, parallel)

		# if mining failed, remember rule length as max tabu value
		if(newSolutionAccuracy<0) {
			tabuRuleLength <- min(newSolution[3], tabuRuleLength)
		}

		# if acceptance -> accept
		if(newSolutionAccuracy >= 0.0 && .acceptance(currentSolutionAccuracy,newSolutionAccuracy,temp)>runif(1,0,1)){
			currentSolution <- newSolution
			currentSolutionAccuracy <- newSolutionAccuracy
		}

		# remember best solution
		if(currentSolutionAccuracy>bestSolutionAccuracy){
			bestSolutionAccuracy <- currentSolutionAccuracy
			bestSolution <- currentSolution
		}

		# break if max achieved
		if(bestSolutionAccuracy==1){
			break
		}

		# remember latest accuracies
		if(currentSolutionAccuracy>0){
			accuracies <- c(accuracies,currentSolutionAccuracy)
		}
		# break if there is only small change of accuracies
		if(bestSolutionAccuracy>0 && accuracies[length(accuracies)]>0 && length(accuracies)>15 && abs(mean(tail(accuracies,15))-bestSolutionAccuracy)<=0.01 && abs(mean(tail(accuracies,15))-bestSolutionAccuracy)>0){
			break
		}

		# if initialization failed -> 100 iteration random search
		if(bestSolutionAccuracy>0 || iteration>100){
			temp <- temp * (1.0-alpha)
		}
		# clean
		gc()
	}

	if(verbose){
	  cat("\n")
	  message(paste(Sys.time()," rCBA: best solution ",paste(bestSolution, collapse=", "),sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}
	start.time <- proc.time()

	output <- list()
	output$iteration <- iteration
	output$confidence <- bestSolution[1]
	output$support <- bestSolution[2]
	output$maxlen <- bestSolution[3]

	# use best parameters
	rules <- suppressWarnings(apriori(as(trainData, "transactions"), parameter = list(confidence = bestSolution[1], support= bestSolution[2], maxlen=bestSolution[3]), appearance = list(rhs = paste(className,unique(trainData[[className]][!is.na(trainData[[className]])]),sep="="), default="lhs"), control = list(verbose = FALSE)))
	if(verbose){
	  message(paste(Sys.time()," rCBA: rules ",length(rules),sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}
	start.time <- proc.time()

	output$initialSize <- length(rules)

	if(pruning==TRUE && length(rules)>0){
		# rulesFrame <- pruning(trainData, rulesFrame, method="m2cba")
		repeating <- TRUE
		while(repeating==TRUE){
			tryCatch({
			  rules <- pruning(trainData, rules, method="m2cba", verbose = FALSE, parallel)
				repeating <- FALSE
			},error=function(e){
			  print(paste(e))
			  print(paste("pruning exception:  ",e))
 			})
		}
	}
	if(verbose){
	  message(paste(Sys.time()," rCBA: pruned rules ",length(rules),sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}
	start.time <- proc.time()

	output$size <- length(rules)
	output$model <- rules
	return(output)
}

.evaluate <- function(conf, supp, maxRuleLen, txns, trainSet, testSet, className, pruning, to=10, parallel = TRUE){
	# initializce
	rules <- NULL
	# timeout limit
	tryCatch({
		# rules <- .processWithTimeout(function()
		rules <- withTimeout({
		  suppressWarnings(apriori(txns, parameter = list(confidence = conf, support= supp, maxlen=maxRuleLen), appearance = list(rhs = paste(className,unique(trainSet[[className]][!is.na(trainSet[[className]])]),sep="="), default="lhs"), control = list(verbose = FALSE)))
		#  , timeout=to)
		}, timeout = to, onTimeout="error");
	}, TimeoutException = function(e){
		print("TimeoutException")
	}, error=function(ex) {
	  if (!"TimeoutException"  %in% class(ex)) {
	    stop(ex)
	  }
	  message("Timeout")
		# if (conditionMessage(ex) == "timeout") {
		# 	print("TimeoutException")
  	#	} else {
    # 		stop(ex)
  	#	}
	})
	# mining failed or too many genereated rules
	if(is.null(rules) || length(rules)>1e5) {
		return(-1)
	}
	# pruning
	if(pruning==TRUE && length(rules)>0){
		repeating <- TRUE
		while(repeating==TRUE){
			tryCatch({
			  rules <- pruning(trainSet, rules, method="m2cba", verbose = FALSE, parallel)
				repeating <- FALSE
			},error=function(e){
 				print("pruning exception")
 			})
		}
	}
	# classification and compute accuracy
	if(length(rules)>0){
		predictions <- classification(testSet, rules, verbose = FALSE)
		accuracy <- sum(as.character(testSet[[className]])==predictions, na.rm=TRUE) / length(predictions)
		return(accuracy)
	} else {
		return(0.0)
	}
}

.acceptance <- function(acc, newAcc, temp){
	# for improved solution -> top acceptance
	if(newAcc > acc){
		return(1.0)
	}
	# otherwise compute acceptance
	return(exp((newAcc*100.0 - acc*100.0) / temp))
}

.processWithTimeout <- function(fun, timeout=30) {
	# implemented by https://github.com/propi
	setTimeLimit(timeout+5);
	myfork <- parallel::mcparallel({
		fun()
	}, silent=FALSE);
	Sys.sleep(0.1);
	myresult <- parallel::mccollect(myfork, wait=FALSE, timeout=timeout);
	tools::pskill(myfork$pid, tools::SIGKILL);
	tools::pskill(-1 * myfork$pid, tools::SIGKILL);
	parallel::mccollect(myfork, wait=FALSE);
	setTimeLimit();
	if(is.null(myresult)){
		stop("timeout", call.=FALSE);
	}
	myresult <- myresult[[1]];
	if(inherits(myresult,"try-error")){
		stop(attr(myresult, "condition"));
	}
	return(myresult);
}

