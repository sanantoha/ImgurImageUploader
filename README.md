How to run: <br>
Before run project need to install: java 8, scala 2.12, sbt, docker <br>
Go to directory with project and run next commands step by step: <br>
Enter to sbt console using command: <br>
sbt <br><br>
Clean project: <br>
clean <br><br>
Compile project: <br>
compile <br><br>
Run tests:<br> 
test <br><br>
Create docker container (docker container is created automatically by plugin: sbt-native-packager):<br> 
docker:publishLocal <br><br>

Check if docker container was created successfully using command: <br>
docker images <br>
You can see something like this:<br>
REPOSITORY <br>
sanantoha/imgurimageuploader <br>

Go to bin directory and run app in docker container using command: <br>
./start.sh <br>

Check query.txt in bin directory for make request to app <br>