# Synopsis

It would be trivial to Dockerize this jar.

```
sbt assembly
java -jar app.jar
curl -i -X POST 127.0.0.1:8080/compute \
  -H "Content-Type: application/json" \
  --data-binary "@./input.json"
```

# Customization

The following environmental variables can be
customized to achieve performance/accuracy:

	- maxError - the error that has to be reached
		in order for the solution to be considered
		accurate enough
	
	- maxDepth - number of maximum heap calls
	
	- boundSteps - number of steps required to find
		the bound; given f(x), the complexity is O(f(x))

# Algorithm explanation

The secant method is used to find the root to the equation. In order
to find the bounds around the root, a random algorithm is used to find a
change in sign between `f(0)` and `f(b)`, which is then segmented into
a number of intervals to find a better granularity. After that, the
secant method is applied until the error is acceptable (or until there 
is an error)

# Known issues

There seem to be some issues regarding the APR calculation. The theory
seems correct, but there might be a minor detail which I might have missed.


