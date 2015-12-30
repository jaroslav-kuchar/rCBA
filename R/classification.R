#' A classification function
#'
#' @param test data.frame with test data
#' @param rules data.frame with rules
#' @return vector with classifications
#' @export
#' @examples
#' library("arules")
#' library("rCBA")
#' data("iris")
#' 
#' train <- sapply(iris,as.factor)
#' train <- data.frame(train, check.names=FALSE)
#' txns <- as(train,"transactions")
#' 
#' rules = apriori(txns, parameter=list(support=0.03, confidence=0.03, minlen=2), 
#'	appearance = list(rhs=c("Species=setosa", "Species=versicolor", "Species=virginica"),default="lhs"))
#' rulesFrame <- as(rules,"data.frame")
#' 
#' predictions <- classification(train,rulesFrame)
#' table(predictions)
#' sum(train$Species==predictions,na.rm=TRUE)/length(predictions)
#' @include init.R
classification <- function(test, rules){
	# init java
	init()
	print(paste(Sys.time()," rCBA: initialized",sep=""))
	# init interface
	jPruning <- J("cz.jkuchar.rcba.r.RSpring")$initializePruning()
	# set column names
	.jcall(jPruning, , "setColumns", .jarray(colnames(test)))

	# add test items
	testConverted <- data.frame(lapply(test, as.character), stringsAsFactors=FALSE)
	testArray <- .jarray(lapply(testConverted, .jarray))
	.jcall(jPruning,,"addDataFrame",testArray)	
	print(paste(Sys.time()," rCBA: dataframe ",nrow(test),"x",ncol(test),sep=""))

	# add rules
	rulesFrame <- as(rules,"data.frame")
	rulesFrame$rules <- as.character(rulesFrame$rules)
	rulesArray <- .jarray(lapply(rulesFrame, .jarray))
	.jcall(jPruning,,"addRuleFrame",rulesArray)
	print(paste(Sys.time()," rCBA: rules ",nrow(rules),"x",ncol(rules),sep=""))

	# perform classification
	jResult <- .jcall(jPruning, "[S", "classify")
	print(paste(Sys.time()," rCBA: classification completed",sep=""))
	# build output
	jResult
}

