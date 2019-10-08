# HiDALGO-gpt
Graph processing tool suite for the [HiDALGO project][1] supporting basic operations for reading, writing and working with graphs.

## Usage

HiDALGO-gpt can perform various tasks. Each one has a class in src/tasks:

* `FilterStarlike.java`: Due to the nature of our data and the chosen clustering algorithm, the dataset might be polluted with starlike graphs. The clustering algorithm seems to have a preference for throwing poorly connected nodes together, which results in a "community" where all nodes are connected to a central node, but there are barely any edges beyond that. This task uses heuristics to discard graphs that qualify as "starlike".
* `FindSpecificEVs.java`: Another task used for inspection of the dataset, this class simply looks for communities that have a specific eigenvalue (minus some delta).
* `MergeOverlappingCommunities.java`: This is the biggest task. The idea is to merge similar communities, as lots of these might emerge since we are clustering each node's neighborhood. There are various parameters to play around with, documented in the class.  
* `PrecalculateAllEVs.java`: Calculates the eigenvalues of a list of communities so that it doesn't have to be computed on the fly.

To run a task, compile the project and call `java tasks/[task]`.

[1]: https://hidalgo-project.eu
