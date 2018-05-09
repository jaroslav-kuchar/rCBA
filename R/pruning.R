#' A Pruning function
#'
#' @param train trainData \code{data.frame} or \code{transactions} from \code{arules} with input data
#' @param rules \code{data.frame} with rules
#' @param method pruning method m2cba(default)|m1cba|dcbrcba
#' @param verbose verbose indicator
#' @param parallel parallel indicator
#' @return \code{data.frame} with pruned rules
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
#'
#' print(length(rules))
#' prunedRules <- rCBA::pruning(train, rules, method="m2cba", parallel=FALSE)
#' print(length(prunedRules))
#' @include init.R
pruning <- function(train, rules, method="m2cba", verbose = TRUE, parallel=TRUE){
	init()
  if(verbose){
	  message(paste(Sys.time()," rCBA: initialized",sep=""))
	  start.time <- proc.time()
  }
	# init interface
	jPruning <- .jnew("cz/jkuchar/rcba/r/RPruning")
	.jcall(jPruning, , "setParallel", parallel)

	if(is(train,"transactions")){
	  # extract vars
	  levels <- unname(sapply(train@itemInfo$labels,function(x) strsplit(x,"=")[[1]][2]))
	  variables <- unname(sapply(train@itemInfo$labels,function(x) strsplit(x,"=")[[1]][1]))
	  # set column names
	  .jcall(jPruning, , "setColumns", .jarray(variables))
	  # set values
	  .jcall(jPruning, , "setValues", .jarray(levels))
	  # add data
	  .jcall(jPruning,,"addTransactionMatrix",.jarray(apply(as(t(train@data),"matrix"),1, .jarray)))
	} else {
  	# set column names
  	.jcall(jPruning, , "setColumns", .jarray(colnames(train)))
  	# add train items
  	trainConverted <- data.frame(lapply(train, as.character), stringsAsFactors=FALSE)
  	trainArray <- .jarray(lapply(trainConverted, .jarray))
  	.jcall(jPruning,,"addDataFrame",trainArray)
	}
	if(verbose){
	  message(paste(Sys.time()," rCBA: data ",paste(dim(train), collapse="x"),sep=""))
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

	# perform pruning
	start.time <- proc.time()
	jPruned <- .jcall(jPruning, "[Lcz/jkuchar/rcba/rules/Rule;", "prune", as.character(method))
	if(verbose){
	  message(paste(Sys.time()," rCBA: pruning completed",sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}
	# build dataframe
	start.time <- proc.time()
	pruned <- data.frame(rules=rep("", 0), support=rep(0.0, 0), confidence=rep(0.0, 0), lift=rep(0.0, 0), stringsAsFactors=FALSE)
	for(jRule in jPruned){
		pruned[nrow(pruned) + 1,] <- c(.jcall(jRule, "S", "getText"), .jcall(jRule, "D", "getSupport"), .jcall(jRule, "D", "getConfidence"), .jcall(jRule, "D", "getLift"))
	}
	jPruned <- NULL
	J("java.lang.System")$gc()
	if(verbose){
	  message(paste(Sys.time()," rCBA: pruned rules ",nrow(pruned),sep=""))
	  message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
	}
	pruned$support <- as.double(pruned$support)
	pruned$confidence <- as.double(pruned$confidence)
	pruned$lift <- as.double(pruned$lift)
	frameToRules(pruned)
}
