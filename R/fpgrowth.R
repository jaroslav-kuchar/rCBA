#' @title FP-Growth
#' @description FP-Growth algorithm - Jiawei Han, Jian Pei, and Yiwen Yin.
#' Mining frequent patterns without candidate generation. SIGMOD Rec. 29, 2 (2000) <doi:10.1145/335191.335372>
#'
#' @param train \code{data.frame} or \code{transactions} from \code{arules} with input data
#' @param support minimum support
#' @param confidence minimum confidence
#' @param maxLength maximum length
#' @param consequent filter consequent - column name with consequent/target class
#' @param verbose verbose indicator
#' @param parallel parallel indicator
#' @export
#' @examples
#' library("rCBA")
#' data("iris")
#'
#' train <- sapply(iris,as.factor)
#' train <- data.frame(train, check.names=FALSE)
#' txns <- as(train,"transactions")
#'
#' rules = rCBA::fpgrowth(txns, support=0.03, confidence=0.03, maxLength=2, consequent="Species",
#'            parallel=FALSE)
#'
#' predictions <- rCBA::classification(train,rules)
#' table(predictions)
#' sum(as.character(train$Species)==as.character(predictions),na.rm=TRUE)/length(predictions)
#'
#' prunedRules <- rCBA::pruning(train, rules, method="m2cba", parallel=FALSE)
#' predictions <- rCBA::classification(train, prunedRules)
#' table(predictions)
#' sum(as.character(train$Species)==as.character(predictions),na.rm=TRUE)/length(predictions)
#' @include init.R
fpgrowth <- function(train, support = 0.01, confidence = 1.0, maxLength = 5, consequent=NULL, verbose = TRUE, parallel=TRUE){
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
  start.time <- proc.time()
  # perform fpgrowth
  jPruned <- .jcall(jPruning, "[[Ljava/lang/String;", "fpgrowth", support, confidence, as.integer(maxLength), consequent, evalArray=FALSE)
  # print(paste(Sys.time()," rCBA: fpgrowth completed",sep=""))
  rules <- .jevalArray(jPruned,simplify=TRUE)
  colnames(rules) <- c("rules","support","confidence","lift")
  pruned<-as.data.frame(rules,stringsAsFactors=FALSE)
  J("java.lang.System")$gc()
  if(verbose){
    message(paste(Sys.time()," rCBA: rules ",nrow(pruned),sep=""))
    message (paste("\t took:", round((proc.time() - start.time)[3], 2), " s"))
  }
  pruned$support <- as.double(pruned$support)
  pruned$confidence <- as.double(pruned$confidence)
  pruned$lift <- as.double(pruned$lift)
  frameToRules(pruned)
}
