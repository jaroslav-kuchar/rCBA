# package created using:
# http://hilaryparker.com/2014/04/29/writing-an-r-package-from-scratch/
#' @import rJava arules R.utils TunePareto parallel
#' @importFrom utils write.table modifyList
#' @importFrom stats runif setNames
#' @importFrom methods as is

.onLoad <- function(libname, pkgname ){
  .jinit()
  jv <- .jcall("java/lang/System", "S", "getProperty", "java.runtime.version")
  if(substr(jv, 1L, 2L) == "1.") {
    jvn <- as.numeric(paste0(strsplit(jv, "[.]")[[1L]][1:2], collapse = "."))
    if(jvn < 1.8) stop("Java >= 8 is needed for this package but not available")
  }
}

init <- function(){
	# initialize rJava
  .jinit()
  jv <- .jcall("java/lang/System", "S", "getProperty", "java.runtime.version")
  if(substr(jv, 1L, 2L) == "1.") {
    jvn <- as.numeric(paste0(strsplit(jv, "[.]")[[1L]][1:2], collapse = "."))
    if(jvn < 1.8) stop("Java >= 8 is needed for this package but not available")
  }
	# add java implementation to classpath
	.jaddClassPath(dir(paste(path.package("rCBA"), "/java/", sep=""), full.names=TRUE))
	# add jar archives to classpath
	jars <- list.files(paste(path.package("rCBA"), "/java/lib/", sep=""))
	for(jar in jars){
		.jaddClassPath(paste(path.package("rCBA"), "/java/lib/", jar, sep=""))
	}
	# add configuration files to classpath
	confs <- list.files(paste(path.package("rCBA"), "/java/conf/", sep=""))
	for(conf in confs){
		.jaddClassPath(paste(path.package("rCBA"), "/java/conf/", conf, sep=""))
	}
}

transactionsToFrame <- function(txns){
  if(is.null(txns@itemInfo$variables) || is.null(txns@itemInfo$levels)){
    levels <- unname(sapply(txns@itemInfo$labels,function(x) strsplit(x,"=")[[1]][2]))
    variables <- unname(sapply(txns@itemInfo$labels,function(x) strsplit(x,"=")[[1]][1]))
    columnNames <- unique(variables)
    df <- as.data.frame(setNames(replicate(length(columnNames),character(0), simplify = F), columnNames), stringsAsFactors = FALSE)
    apply(txns@data,2, function(x){
      row <- rep(NA,length(columnNames))
      row[match(variables[which(x)],columnNames)] <- levels[which(x)]
      df[nrow(df)+1,] <<- row
      NULL
    })
    colnames(df) <- columnNames
  } else {
    columnNames <- as.character(unique(txns@itemInfo$variables))
    levels <- txns@itemInfo$levels
    df <- as.data.frame(setNames(replicate(length(columnNames),character(0), simplify = F), columnNames), stringsAsFactors = FALSE)
    apply(txns@data,2, function(x){
      row <- as.character(levels[which(x)])
      df[nrow(df)+1,] <<- row
      NULL
    })
    colnames(df) <- columnNames
  }
  df
}
