Before running, install the middleware and associated tools (on both machines):


## On Amazon Linux
```
sudo dnf install https://download.zeroc.com/ice/3.7/amzn2023/ice-repo-3.7.amzn2023.noarch.rpm
```

```
sudo dnf install python3-ice ice-compilers
```

## On Ubuntu
```
wget "https://download.zeroc.com/ice/3.8/ubuntu26.04/ice-repo-3.8_1.0.0_all.deb" -O ice-repo.deb
sudo dpkg -i ice-repo.deb
rm ice-repo.deb
sudo apt-get update
```
```
sudo apt-get install python3-zeroc-ice
```
```
sudo apt-get install zeroc-ice-compilers
```
Note: This code is exactly as in Example 3.21 of Maarten van Steen's book.
