#' A classification function
#'
#' @param test \code{data.frame} or \code{transactions} from \code{arules} with input data
#' @param rules \code{data.frame} with rules
#' @param verbose verbose indicator
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
#'
#' predictions <- rCBA::classification(train,rules)
#' table(predictions)
#' sum(as.character(train$Species)==as.character(predictions),na.rm=TRUE)/length(predictions)
#' @include init.R
classification <- function(test, rules, verbose = TRUE){
	# init java
	init()
	if(verbose){
	  message(paste(Sys.time()," rCBA: initialized",sep=""))
	  start.time <- proc.time()
	}
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
	if(verbose){
	  message(paste(Sys.time()," rCBA: data ",paste(dim(test), collapse="x"),sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}

	# add rules
	start.time <- proc.time()
	rulesFrame <- as(rules,"data.frame")
	rulesFrame$rules <- as.character(rulesFrame$rules)
	rulesArray <- .jarray(lapply(rulesFrame, .jarray))
	.jcall(jPruning,,"addRuleFrame",rulesArray)
	if(verbose){
	  message(paste(Sys.time()," rCBA: rules ",length(rules),sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}

	# perform classification
	start.time <- proc.time()
	jResult <- .jcall(jPruning, "[S", "classify")
	J("java.lang.System")$gc()
	if(verbose){
	  message(paste(Sys.time()," rCBA: classification completed",sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}
	# build output
	factor(jResult)
}

