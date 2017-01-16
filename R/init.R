# package created using:
# http://hilaryparker.com/2014/04/29/writing-an-r-package-from-scratch/
#' @import rJava arules R.utils TunePareto
#' @importFrom utils write.table modifyList
#' @importFrom stats runif setNames
#' @importFrom methods as is

.onLoad <- function(libname, pkgname ){
	.jinit()
	if(J("java.lang.System")$getProperty("java.version") < "1.8.0") {
		stop("rCBA requires Java >= 1.8 ", call. = FALSE)
	}
}

init <- function(){
	# initialize rJava
  .jinit()
	if(J("java.lang.System")$getProperty("java.version") < "1.8.0") {
		stop("rCBA requires Java >= 1.8 ", call. = FALSE)
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
  columnNames <- as.character(unique(txns@itemInfo$variables))
  df <- as.data.frame(setNames(replicate(length(columnNames),character(0), simplify = F), columnNames), stringsAsFactors = FALSE)
  apply(txns@data,2, function(x){
    row <- as.character(txns@itemInfo[which(x),]$levels)
    df[nrow(df)+1,] <<- row
    NULL
  })
  # for(i in 1:length(txns)){
  #   row <- as.character(txns@itemInfo[which(matrix(txns[i]@data)),]$levels)
  #   df[nrow(df)+1,] <- row
  # }
  df
}
