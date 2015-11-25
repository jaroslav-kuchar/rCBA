# package created using:
# http://hilaryparker.com/2014/04/29/writing-an-r-package-from-scratch/

init <- function(){
	# initialize rJava
	library(rJava)
	.jinit()
	# add java implementation to classpath
	.jaddClassPath(dir(paste(path.package("rCBA"), "/java/", sep=""), full.names=TRUE))
	# add jar archives to classpath
	jars <- list.files(paste(path.package("rCBA"), "/java/lib/", sep=""))
	for(jar in jars){
		if(!grepl("validation-api",jar)){
			.jaddClassPath(paste(path.package("rCBA"), "/java/lib/", jar, sep=""))
		}
	}
	# add configuration files to classpath
	confs <- list.files(paste(path.package("rCBA"), "/java/conf/", sep=""))
	for(conf in confs){
		.jaddClassPath(paste(path.package("rCBA"), "/java/conf/", conf, sep=""))
	}
	# J("java.lang.System")$getProperty("java.version")		
}

#' A Pruning function
#'
#' @param train data.frame with training data
#' @param rules data.frame with rules
#' @param method pruning method m2cba(default)|m1cba|dcbrcba
#' @return data.frame with pruned rules
#' @export
#' @examples
#' library("arules")
#' library("rCBA")
#' 
#' train <- read.csv("./train.csv",header=TRUE) # read data
#' 
#' txns <- as(train,"transactions") # convert
#' rules <- apriori(txns, parameter = list(confidence = 0.1, support= 0.1, minlen=1, maxlen=5)) # rule mining
#' rules <- subset( rules, subset = rhs %pin% "y=") # filter
#' rulesFrame <- as(rules,"data.frame") # convert
#' 
#' print(nrow(rulesFrame))
#' prunedRulesFrame <- pruning(trainData, rulesFrame, method="m2cba")
#' print(nrow(prunedRulesFrame))
pruning <- function(train, rules, method="m2cba"){
	# init java
	init()
	print(paste(Sys.time()," rCBA: initialized",sep=""))
	# init interface
	jPruning <- J("cz.jkuchar.rcba.r.RSpring")$initializePruning()
	# set column names
	.jcall(jPruning, , "setColumns", .jarray(colnames(train)))
	# add train items
	f<-tempfile()
	write.table(train, file=f, sep=",", row.names=FALSE, col.names=FALSE)
	trainConverted <- data.frame(lapply(train, as.character), stringsAsFactors=FALSE)
	for(i in 1:nrow(trainConverted)){
		.jcall(jPruning, , "addItem", as.character(unname(unlist(trainConverted[i,]))))
	}
	# .jcall(jPruning, , "loadFromFile", as.character(f))
	print(paste(Sys.time()," rCBA: dataframe ",nrow(train),"x",ncol(train),sep=""))
	# add rules
	for (i in 1:nrow(rules)){
		.jcall(jPruning, , "addRule", as.character(rules[i,]$rules), as.numeric(rules[i,]$confidence), as.numeric(rules[i,]$support), as.numeric(rules[i,]$lift))
	}
	print(paste(Sys.time()," rCBA: rules ",nrow(rules),"x",ncol(rules),sep=""))
	# perform pruning
	jPruned <- .jcall(jPruning, "[Lcz/jkuchar/rcba/rules/Rule;", "prune", as.character(method))
	print(paste(Sys.time()," rCBA: pruning completed",sep=""))
	# build dataframe
	pruned <- data.frame(rules=rep("", 0), support=rep(0.0, 0), confidence=rep(0.0, 0), lift=rep(0.0, 0), stringsAsFactors=FALSE) 
	for(jRule in jPruned){
		pruned[nrow(pruned) + 1,] <- c(.jcall(jRule, "S", "getText"), .jcall(jRule, "D", "getSupport"), .jcall(jRule, "D", "getConfidence"), .jcall(jRule, "D", "getLift"))
	}
	print(paste(Sys.time()," rCBA: pruned rules ",nrow(pruned),"x",ncol(pruned),sep=""))
	pruned$support <- as.double(pruned$support)
	pruned$confidence <- as.double(pruned$confidence)
	pruned$lift <- as.double(pruned$lift)
	pruned
}