#!/bin/bash

echo "deb http://cran.rstudio.com/bin/linux/ubuntu trusty/" | tee /etc/apt/sources.list.d/rstudio.list

gpg --keyserver keyserver.ubuntu.com --recv-key E084DAB9
gpg -a --export E084DAB9 | sudo apt-key add -

echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list
echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886

apt-get update
apt-get -y install r-base
apt-get -y install r-dev
apt-get -y install libcurl4-gnutls-dev
apt-get -y install libxml2-dev
apt-get -y install libssl-dev

echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections

apt-get -y install oracle-java8-installer
apt-get -y install oracle-java8-set-default

R CMD javareconf

R -e "install.packages(c('devtools', 'roxygen2'), repos='http://cran.rstudio.com/')"
R -e "install.packages('rJava', repos='http://cran.rstudio.com/')"

apt-get -y install libapparmor1 gdebi-core
wget https://download2.rstudio.org/rstudio-server-1.1.447-amd64.deb
gdebi --n rstudio-server-1.1.447-amd64.deb

apt-get -y install maven