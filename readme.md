# friends
A program that fetches lets you fetch and analyze information about your facebook friends network. This program only retrieves information that the other user has made available to you (i.e. private information will not be obtained).

## Usage
[include stuff about how to use this later]

## Compatibility
The program (currently) only runs correctly on Windows 10. Note that there could possibly be a bug with the `java.awt.Robot` class when running on Windows 10 when the display scaling is not at its default value (100% for most desktops and 125% for most laptops).

## Files
- `Person.java`: A class that stores various information about a person, such as `id`, `name`, and `url`.
- `Graph.java`: A class that represents an undirected graph. Includes various graph operations and algorithms.
- `InterruptibleRobot.java`: A class that wraps/extends a `java.awt.Robot` such that it can be interrupted by manually moving the mouse.
- `Harvester.java`: A class that is used to download the dynamically generated `.html` file of a facebook user's Friends page (with all friends loaded on the page), using an `InterruptibleRobot`.
- `FriendsHtmlParser.java`: A class that is used to parse the `.html` Friends page that is obtained from `Harvester#beginNewHarvest()` and `Harvester#harvestAllPages`, extracting information such as a list of the user's friends.
- `Main.java`: The executable class.

## Notes
- More detail about how classes are implemented can be found in the source code.
- Many design choices made here may seem unoptimal, but were chosen given the restriction that facebook doesn't (to my knowledge as of now) allow access to other people's friends through an API and they also have some server-side prevention measures against automated information retrieval (ex: using a wget). Still, there are clearly better ways to implement some of this program's functionalities, but the current implementation is simple to understand (not dependent on any non-standard libraries) and "good enough".

### Development/testing/benchmarking notes: using i5-7400 (2 core 4 thread @ 3.00 GHz)
- `Harvester(maxNumPeople=250, maxPerPerson=100)`: 8440 seconds
- `findCliques()` with `|V| = (5660/250)`, `|E| = 17600` (where `(a/b)` means `a` total nodes and `b` nodes that we have complete information about): 252 seconds
- 250 small .html files along with the _files source folder: 3.5 GB

## Bugs
- During development, lots of bugs were observed relating to copying and pasting the incorrect Strings from clipboard. I suspect that it has to do with previous instances of `Harvester` and `InterruptibleRobot`s (or other `Robot`s) whose threads were not closed and thus continued to perform copy/paste operations while the current program was running. These can be seen on Windows Task Manager (as a running java binary).

## TODO, possible improvements
- Revamp file I/O to only use java's files and paths libraries (`java.nio.\*`)
- Fix some potentially confusing code relating to mixing up `Path` vs `String` as arguments
- Use asynchronous functions and callbacks to improve speed and reliability (currently, the program usually calls Thread.sleep() if it needs to wait for a process to finish)
