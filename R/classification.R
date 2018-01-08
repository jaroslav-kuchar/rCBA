#' A classification function
#'
#' @param test \code{data.frame} or \code{transactions} from \code{arules} with input data
#' @param rules \code{data.frame} with rules
#' @return vector with classifications
#' @export
#' @examples
#' library("arules")
#' library("rCBA")
#' data("iris")
#'
#' train <- sapply(iris, as.factor)
#' train <- data.frame(train, check.names=FALSE)
#' txns <- as(train,"transactions")
#'
#' rules = apriori(txns, parameter=list(support=0.03, confidence=0.03, minlen=2),
#'	appearance = list(rhs=c("Species=setosa", "Species=versicolor", "Species=virginica"),default="lhs"))
#' rulesFrame <- as(rules,"data.frame")
#'
#' predictions <- rCBA::classification(train,rulesFrame)
#' table(predictions)
#' sum(train$Species==predictions,na.rm=TRUE)/length(predictions)
#' @include init.R
classification <- function(test, rules){
	# init java
	init()
	print(paste(Sys.time()," rCBA: initialized",sep=""))
	# init interface
	jPruning <- .jnew("cz/jkuchar/rcba/r/RPruning")
	if(is(test,"transactions")){
	  # extract vars
	  levels <- unname(sapply(test@itemInfo$labels,function(x) strsplit(x,"=")[[1]][2]))
	  variables <- unname(sapply(test@itemInfo$labels,function(x) strsplit(x,"=")[[1]][1]))
	  # set column names
	  .jcall(jPruning, , "setColumns", .jarray(variables))
	  # set values
	  .jcall(jPruning, , "setValues", .jarray(levels))
	  # add data
	  .jcall(jPruning,,"addTransactionMatrix",.jarray(apply(as(t(test@data),"matrix"),1, .jarray)))
	} else {
	  # set column names
	  .jcall(jPruning, , "setColumns", .jarray(colnames(test)))
	  # add test items
	  testConverted <- data.frame(lapply(test, as.character), stringsAsFactors=FALSE)
	  testArray <- .jarray(lapply(testConverted, .jarray))
	  .jcall(jPruning,,"addDataFrame",testArray)
	}
	print(paste(Sys.time()," rCBA: data ",paste(dim(test), collapse="x"),sep=""))

	# add rules
	rulesFrame <- as(rules,"data.frame")
	rulesFrame$rules <- as.character(rulesFrame$rules)
	rulesArray <- .jarray(lapply(rulesFrame, .jarray))
	.jcall(jPruning,,"addRuleFrame",rulesArray)
	print(paste(Sys.time()," rCBA: rules ",nrow(rules),"x",ncol(rules),sep=""))

	# perform classification
	jResult <- .jcall(jPruning, "[S", "classify")
	J("java.lang.System")$gc()
	print(paste(Sys.time()," rCBA: classification completed",sep=""))
	# build output
	jResult
}

