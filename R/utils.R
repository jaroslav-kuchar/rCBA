#' Conversion of \code{data.frame} to \code{rules} from \code{arules}
#'
#' @param model \code{data.frame} with rules
#' @return \code{arules} \code{rules} representation
#' @export
#' @examples
#' library("rCBA")
#' data("iris")
#'
#' output <- rCBA::buildFPGrowth(iris[sample(nrow(iris), 50),], "Species")
#' model <- output$model
#'
#' rules <- rCBA::frameToRules(model)
#' inspect(rules)
#'
#' @include init.R
frameToRules <- function(model){
  # export quality measures
  quality<-model[,2:4]
  # parse text
  rowItems <- lapply(model$rules,function(x) {
    x <- as.character(x)
    pattern <- "[[:space:]]*\\{(.*)\\}[[:space:]]*=>[[:space:]]*\\{(.*)\\}[[:space:]]*"
    m <- regexec(pattern, x)
    strRule <- regmatches(x, m)
    ant <- strsplit(strRule[[1]][2],",")[[1]]
    cons <- strsplit(strRule[[1]][3],",")[[1]]
    list(ant=ant,cons=cons)
  })
  # unique lhs and rhs items
  antItems <- unique(unlist(sapply(rowItems, function(x) x$ant)))
  consItems <- unique(unlist(sapply(rowItems, function(x) x$cons)))
  # all items
  items <- c(antItems, consItems)
  # prepare matrices for antecedents(lhs) and consequents(rhs)
  antM <- matrix(0, ncol=length(items), nrow = nrow(model))
  dimnames(antM) <- list(NULL, items)
  consM <- matrix(0, ncol=length(items), nrow = nrow(model))
  dimnames(consM) <- list(NULL, items)
  # set presence of items in lhs and rhs
  sapply(seq_len(nrow(model)), function(x){
    row <- unname(rowItems[x])[[1]]
    antM[x,match(row$ant,items)] <<- 1
    consM[x,match(row$cons,items)] <<- 1
    NULL
  })
  # convert to item matrix
  antI <- as(antM, "itemMatrix")
  consI <- as(consM, "itemMatrix")
  # create rules
  rules <- new("rules", lhs=antI, rhs=consI, quality = quality)
  rules
}
