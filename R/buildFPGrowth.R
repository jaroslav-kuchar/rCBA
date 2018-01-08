#' @title Build classifier function (FP-Growth-based)
#' @description Automatic build of the classification model using the FP-Growth algorithm
#'
#' @param train \code{data.frame} or \code{transactions} from \code{arules} with input data
#' @param className column name with the target class - default is the last column
#' @return list with parameters and model as data.frame with rules
#' @export
#' @examples
#' library("rCBA")
#' data("iris")
#'
#' output <- rCBA::buildFPGrowth(iris[sample(nrow(iris), 50),], "Species")
#' model <- output$model
#'
#' predictions <- rCBA::classification(iris, model)
#' table(predictions)
#' sum(iris$Species==predictions, na.rm=TRUE) / length(predictions)
#'
#' @include init.R
buildFPGrowth <- function(train, className=NULL){
  init()
  print(paste(Sys.time()," rCBA: initialized",sep=""))
  # init interface
  jPruning <- .jnew("cz/jkuchar/rcba/r/RPruning")

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
  print(paste(Sys.time()," rCBA: data ",paste(dim(train), collapse="x"),sep=""))
  # perform fpgrowth
  jPruned <- .jcall(jPruning, "[[Ljava/lang/String;", "build", className, evalArray=FALSE)
  print(paste(Sys.time()," rCBA: fpgrowth completed",sep=""))
  rules <- .jevalArray(jPruned,simplify=TRUE)
  if(nrow(rules)==0){
    pruned <- data.frame(rules=rep("", 0), support=rep(0.0, 0), confidence=rep(0.0, 0), lift=rep(0.0, 0), stringsAsFactors=FALSE)
  } else {
    colnames(rules) <- c("rules","support","confidence","lift")
    pruned<-as.data.frame(rules,stringsAsFactors=FALSE)
  }
  J("java.lang.System")$gc()
  print(paste(Sys.time()," rCBA: mined rules ",nrow(pruned),"x",ncol(pruned),sep=""))
  pruned$support <- as.double(pruned$support)
  pruned$confidence <- as.double(pruned$confidence)
  pruned$lift <- as.double(pruned$lift)
  output <- list()
  output$model <- pruned
  output
}
