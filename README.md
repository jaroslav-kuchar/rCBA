# rCBA

CBA classifier for R - provides implementations of a classifier based on the "Classification Based on Associations" (CBA). It can be used for building classification models from association rules. Rules are pruned in the order of precedence given by the sort criteria and a default rule is added. The final classifier labels provided instances. CBA was originally proposed by Liu, B. Hsu, W. and Ma, Y (1998). Integrating Classification and Association Rule Mining. Proceedings KDD-98, New York, 27-31 August. AAAI. pp80-86.

If you publish your research that uses rCBA, please cite:
```bib
@inproceedings{Kuchar:2015:EasyMiner,
  author    = {Stanislav Vojir and Vaclav Zeman and Jaroslav Kuchar and Tomas Kliegr},
  title     = {EasyMiner/R Preview: Towards a Web Interface for Association Rule Learning and Classification in R},
  booktitle = {Proceedings of the RuleML 2015 Challenge, the Special Track on Rule-based Recommender Systems for the Web of Data, the Special Industry Track and the RuleML 2015 Doctoral Consortium hosted by the 9th International Web Rule Symposium (RuleML 2015), Berlin, Germany, August 2-5, 2015.},
  year      = {2015}
}
```

## Installation

The package is available in CRAN repository:

- https://cran.r-project.org/web/packages/rCBA/index.html

```R
install.packages('rCBA',dependencies=TRUE, repos="http://cran.us.r-project.org")
```

## Development Version Installation

Prerequisites:

- Java 8
- R packages - devtools, rJava, R.utils

R dependencies installation:
```R
install.packages(c("devtools","rJava"),dependencies=TRUE, repos="http://cran.us.r-project.org")
```

Reconfiguration of Java in R:
```bash
sudo R CMD javareconf
```

Recompile and reinstall rJava:
```R
install.packages('rJava', type='source', dependencies=TRUE, repos="http://cran.us.r-project.org")
```

rCBA installation:
```R
library("devtools")
devtools::install_github("jaroslav-kuchar/rCBA")
```

## Usage

Example 1 - automatically build model + pruning + classification:

```R
library("rCBA")
data("iris")

train <- sapply(iris, as.factor)
train <- data.frame(train, check.names=FALSE)

model <- rCBA::build(train)

predictions <- rCBA::classification(train, model)
table(predictions)
sum(train$Species==predictions, na.rm=TRUE) / length(predictions)

prunedModel <- rCBA::pruning(train, model)

predictions <- rCBA::classification(train, prunedModel)
table(predictions)
sum(train$Species==predictions, na.rm=TRUE) / length(predictions)
```

Example 2 - apriori + pruning:

```R
library("arules")
library("rCBA")

train <- read.csv("./train.csv",header=TRUE) # read data

txns <- as(train,"transactions") # convert
rules <- apriori(txns, parameter = list(confidence = 0.1, support= 0.1, minlen=1, maxlen=5)) # rule mining
rules <- subset( rules, subset = rhs %pin% "y=") # filter
rulesFrame <- as(rules,"data.frame") # convert

print(nrow(rulesFrame))
prunedRulesFrame <- pruning(train, rulesFrame, method="m2cba") # m2cba(default)|m1cba|dcbrcba
print(nrow(prunedRulesFrame))
```

Example 3 - apriori + classification: 

```R
library("arules")
library("rCBA")
data("iris")

train <- sapply(iris,as.factor)
train <- data.frame(train, check.names=FALSE)
txns <- as(train,"transactions")

rules = apriori(txns, parameter=list(support=0.03, confidence=0.03, minlen=2), 
	appearance = list(rhs=c("Species=setosa", "Species=versicolor", "Species=virginica"),default="lhs"))
rulesFrame <- as(rules,"data.frame")

predictions <- classification(train,rulesFrame)
table(predictions)
sum(train$Species==predictions,na.rm=TRUE)/length(predictions)

prunedRulesFrame <- pruning(train, rulesFrame, method="m2cba")
predictions <- classification(train, prunedRulesFrame)
table(predictions)
sum(train$Species==predictions,na.rm=TRUE)/length(predictions)
```

## Contributors

- Jaroslav Kuchař (https://github.com/jaroslav-kuchar)

## Licence

Apache License Version 2.0
