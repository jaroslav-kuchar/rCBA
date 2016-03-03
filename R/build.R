#' A build classifier function
#'
#' @param trainData data.frame with train data
#' @param className column with target class - default is the last column
#' @param pruning performing pruning while building the model
#' @return data.frame with rules
#' @export
#' @examples
#' library("rCBA")
#' data("iris")
#' 
#' model <- rCBA::build(iris)
#' 
#' predictions <- rCBA::classification(iris, model)
#' table(predictions)
#' sum(iris$Species==predictions, na.rm=TRUE) / length(predictions)
#' 
#' @include init.R
build <- function(trainData, className=NA, pruning=TRUE){
	print(paste(Sys.time(), " rCBA: initialized", sep=""))

	# classname
	if(!exists("className") || is.na(className)){
		className <- tail(names(trainData),1)
	}

	# preprocess
	trainData <- sapply(trainData,as.factor)
	trainData <- data.frame(trainData, check.names=F)
	
	# create train and test fold using stratified 2fold
	folds <- generateCVRuns(labels = trainData[[className]],ntimes = 1,nfold = 4,stratified=TRUE)
	testIndex <- folds[[1]][[1]]
	# create train and test fold using random sample
	# index <- 1:nrow(trainData)
	# testIndex <- sample(index, trunc(length(index)/4))
	# testIndex <- testIndex[!is.na(testIndex)]
	testSet <- trainData[testIndex,]
	trainSet <- trainData[-testIndex,]
	# convert the trainset to transactions
	txns <- as(trainSet, "transactions")

	print(paste(Sys.time()," rCBA: dataframe ",nrow(trainData),"x",ncol(trainData),sep=""))

	# initial temperature
	temp <- 100.0
	# cooling parameter
	alpha <- 0.05
	# tabu rule length - max rule length
	tabuRuleLength <- 5
	# current and best solution
	currentSolution <- c(runif(1,0,1),runif(1,0,1),round(runif(1,1,min(ncol(trainData),tabuRuleLength))))
	currentSolutionAccuracy <- .evaluate(currentSolution[1], currentSolution[2], currentSolution[3], txns, trainSet, testSet, className, pruning)
	bestSolution <- currentSolution
	bestSolutionAccuracy <- currentSolutionAccuracy	

	accuracies <- c(currentSolutionAccuracy)	
	iteration <- 0

	# start
	while(temp>1.0){
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
					direction <- min(newSolution[parameter]/3.0, 0.1)
				} else if(currentSolutionAccuracy==0){
					# no results yet - decrease support/confidence
					direction <- -min(newSolution[parameter]/3.0, 0.1)
				} else {
					# otherwise - random
					direction <- runif(1,-min(newSolution[parameter]/3.0, 0.1), min(newSolution[parameter]/3.0, 0.1))
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
					# mining failed -> shortef rules
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
		newSolutionAccuracy <- .evaluate(newSolution[1], newSolution[2], newSolution[3], txns, trainSet, testSet, className, pruning)

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

	print(paste(Sys.time()," rCBA: best solution ",bestSolution,sep=""))

	# use best parameters
	rules <- apriori(as(trainData, "transactions"), parameter = list(confidence = bestSolution[1], support= bestSolution[2], maxlen=bestSolution[3]), appearance = list(rhs = paste(className,unique(trainData[[className]]),sep="="), default="lhs"))	
	rulesFrame <- as(rules, "data.frame")
	print(paste(Sys.time()," rCBA: rules ",nrow(rulesFrame),"x",ncol(rulesFrame),sep=""))
	if(pruning==TRUE && nrow(rulesFrame)>0){
		rulesFrame <- pruning(trainSet, rulesFrame, method="m2cba")
	}
	print(paste(Sys.time()," rCBA: rules ",nrow(rulesFrame),"x",ncol(rulesFrame),sep=""))
	rulesFrame
}

.evaluate <- function(conf, supp, maxRuleLen, txns, trainSet, testSet, className, pruning){
	# initializce
	rules <- NULL
	# timeout limit
	tryCatch({
		# rules <- evalWithTimeout({
		# 	apriori(txns, parameter = list(confidence = conf, support= supp, maxlen=maxRuleLen), appearance = list(rhs = paste(className,unique(trainSet[[className]]),sep="="), default="lhs"))
		# }, timeout=10)
		rules <- .processWithTimeout(function() apriori(txns, parameter = list(confidence = conf, support= supp, maxlen=maxRuleLen), appearance = list(rhs = paste(className,unique(trainSet[[className]]),sep="="), default="lhs")), timeout=10)
	}, TimeoutException = function(e){
		print("TimeoutException")
	})
	# mining failed or too many genereated rules 
	if(is.null(rules) || length(rules)>1e5) {
		return(-1)
	}
	# convert
	rulesFrame <- as(rules, "data.frame")	
	# pruning
	if(pruning==TRUE && nrow(rulesFrame)>0){
		rulesFrame <- pruning(trainSet, rulesFrame, method="m2cba")
	}
	# classification and compute accuracy
	if(nrow(rulesFrame)>0){
		predictions <- classification(testSet, rulesFrame)
		accuracy <- sum(testSet[[className]]==predictions, na.rm=TRUE) / length(predictions)
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
	return(exp((newAcc*10.0 - acc*10.0) / temp))
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

