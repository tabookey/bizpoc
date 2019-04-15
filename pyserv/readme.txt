1. Create environment for server:
	virtualenv venv
2. start env: 
	source ./venv/bin/activate
3. install apps from requirements.txt:
	pip install flask

4. now run server:
	./run.sh run
 or run debug server:
	./debug.sh run

to show configured server paths, use "./debug.sh routes"
