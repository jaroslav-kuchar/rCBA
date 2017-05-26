#' FP-Growth mining
#'
#' @param train data.frame or transactions with training data
#' @param support minimum support
#' @param confidence minimum confidence
#' @param maxLength maximum length
#' @param consequent filter consequent
#' @export
#' @include init.R
fpgrowth <- function(train, support = 0.01, confidence = 1.0, maxLength = 5, consequent=NULL){
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
  jPruned <- .jcall(jPruning, "[[Ljava/lang/String;", "fpgrowth", support, confidence, as.integer(maxLength), consequent, evalArray=FALSE)
  print(paste(Sys.time()," rCBA: fpgrowth completed",sep=""))
  rules <- .jevalArray(jPruned,simplify=TRUE)
  colnames(rules) <- c("rules","support","confidence","lift")
  pruned<-as.data.frame(rules,stringsAsFactors=FALSE)
  J("java.lang.System")$gc()
  print(paste(Sys.time()," rCBA: mined rules ",nrow(pruned),"x",ncol(pruned),sep=""))
  pruned$support <- as.double(pruned$support)
  pruned$confidence <- as.double(pruned$confidence)
  pruned$lift <- as.double(pruned$lift)
  pruned
}
