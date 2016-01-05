#' A build classifier function
#'
#' @param trainData data.frame with train data
#' @param className column with target class - default is the last column
#' @param maxRules maximum rules in the build model
#' @return data.frame with rules
#' @export
#' @examples
#' library("rCBA")
#' data("iris")
#' 
#' train <- sapply(iris, as.factor)
#' train <- data.frame(train, check.names=FALSE)
#' 
#' model = rCBA::build(train)
#' 
#' predictions <- rCBA::classification(train, model)
#' table(predictions)
#' sum(train$Species==predictions, na.rm=TRUE) / length(predictions)
#' 
#' prunedModel <- rCBA::pruning(train, model)
#' 
#' predictions <- rCBA::classification(train, prunedModel)
#' table(predictions)
#' sum(train$Species==predictions, na.rm=TRUE) / length(predictions)
#' @include init.R
build <- function(trainData, className=NA, maxRules=10000){
	print(paste(Sys.time(), " rCBA: initialized", sep=""))

	# preprocess
	train <- sapply(trainData, as.factor)
	train <- data.frame(train, check.names=F)
	txns <- as(train, "transactions")

	if(!exists("className") || is.na(className)){
		className <- tail(names(train),1)
	}

	print(paste(Sys.time()," rCBA: dataframe ",nrow(train),"x",ncol(train),sep=""))

	# mine rules
	rules <- new("rules", lhs=new("itemMatrix"), rhs=new("itemMatrix"))
	tryCatch({
		for (v in seq(from = 1.0, to = 0.0, by = -0.2)){
			conf <- v
			supp <- v
			for (i in 1:ncol(trainData)) {
				tempRules <- evalWithTimeout({
					apriori(txns, parameter = list(confidence = conf, support= supp, minlen=i, maxlen=i))
				}, timeout=10)
				if(is.null(tempRules)){
					break
				}
				tempRules <- subset(tempRules, subset = rhs %pin% paste(className,"=",sep="")) # filter	
				if (length(rules) > 0) {
					rules <- union(rules, tempRules)
				} else {
					rules <- tempRules	
				}
				if (length(rules) >= maxRules || length(tempRules)==0) {
			        break
			    }    
			    gc()
			}
			if (length(rules) >= maxRules || is.null(tempRules)) {
				break
			}   
		}
	}, TimeoutException = function(e){
		print("TimeoutException")
	}, error = function(e){
		print("Error")
	})

	# convert
	rules <- sort(rules, by = "support") # sort
	rules <- sort(rules, by = "confidence") # sort
	rulesFrame <- as(rules, "data.frame") # convert

	print(paste(Sys.time()," rCBA: rules ",nrow(rulesFrame),"x",ncol(rulesFrame),sep=""))
	# max rules count
	if (nrow(rulesFrame) >= maxRules) {
		rulesFrame <- head(rulesFrame[with(rulesFrame, order(-confidence, -support)), ], maxRules)
	}
	print(paste(Sys.time()," rCBA: rules ",nrow(rulesFrame),"x",ncol(rulesFrame),sep=""))
	rulesFrame
}

