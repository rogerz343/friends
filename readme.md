# friends
A program that fetches lets you fetch and analyze information about your facebook friends network. This program only retrieves information that the other user has made available to you (i.e. private information will not be obtained).

## Usage
### Setup
- Your computer is running Windows 10, with default sized taskbar.
- The display resolution is 1920 x 1080, with Windows scaling at 100% (but see note in the "Compatibility" section). Additional displays may also be present (ex: in a dual-monitor setup) but this program should be running on the left-most display so that the upper-left corner has coordinates (0, 0).
- The browser that is on screen is Google Chrome.
- Google Chrome is maximized on screen.
- The bookmarks bar is visible (ctrl-shift-b to toggle this).
- The downloads bar is visible (download anything to make it visible, e.g. any website's html file).
### Running the code
Specify:
- The path to the location where files downloaded in Google Chrome are saved by default.
- The path to a directory where the outputs of this program will save to.
- The maximum number of Friends pages to download.
- [optional (?)] The maximum number of friends to retrieve for each person.

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

## Development/testing/benchmarking notes: using i5-7400 (2 core 4 thread @ 3.00 GHz)
### Harvester
- `Harvester(maxNumPeople=350, maxPerPerson=2000)`: 34000 seconds (approx 9.4 hours)
- `Harvester(maxNumPeople=250, maxPerPerson=100)`: 8440 seconds (approx 2.3 hours)
### Finding Cliques: G = (V, E) and V = X ∪ Y and X ∩ Y = ∅, where X is the set of nodes that we have complete information about, and Delta is an (approximate) upper bound on the degree of any vertex in the graph.
- `maximalCliquesContaining()` with `|X| = 440, |Y| = 0, |E| = 15000`
  - num recursive calls:
  - run time: 435 seconds
  - num maximal cliques of size 1 or 2:
  - num maximal cliques of size >= 3:
  - maximum clique size:
- `allMaximalCliques()` with `|X| = 450, |Y| = 181700, |E| = 298252, Delta = 2000`:
  - num recursive calls:
  - run time:
  - time to process first 10000 nodes: 385 seconds
  - time to process first 100000 nodes: 3924 seconds
  - time to process first 181000 nodes: 7549 seconds
  - time to process last 1000 nodes: [untested, manually terminated after 6 hours]
  - num maximal cliques of size 1 or 2:
  - num maximal cliques of size >= 3:
  - maximum clique size:
- `allMaximalCliques()` with `|X| = 250, |Y| = 5660, |E| = 17600, Delta = 100`:
  - num recursive calls: 35787174
  - run time: 237 seconds (2 seconds for first 5000 nodes + 235 seconds for remaining 660)
  - num maximal cliques of size 1 or 2: 5037
  - num maximal cliques of size >= 3: 127867
  - maximum clique size: 37
### Storage
- 250 small .html files along with the _files source folder: 3.5 GB
- 350 near-complete .html files along with the _files source folder: 8 GB

## Bugs
- During development, lots of bugs were observed relating to copying and pasting the incorrect Strings from clipboard. I suspect that it has to do with previous instances of `Harvester` and `InterruptibleRobot`s (or other `Robot`s) whose threads were not closed and thus continued to perform copy/paste operations while the current program was running. These can be seen on Windows Task Manager (as a running java binary).
- Occasionally, the program may (for unknown reason) switch to downloading to Desktop, instead of Downloads folder, which prevents data collection from that point onwards. Current guess is that this happens when the Downloads folder becomes too large.

## TODO, possible improvements
- Allow user to decide where to download .html files, and enforce this in the code.
- Allow user to click on the screen where some (currently hardcoded) coordinates are (e.g. scroll bar bottom position).
- Revamp file I/O to only use java's files and paths libraries (`java.nio.\*`)
- Fix some potentially confusing code relating to mixing up `Path` vs `String` as arguments
- Use asynchronous functions and callbacks to improve speed and reliability (currently, the program usually calls Thread.sleep() if it needs to wait for a process to finish)