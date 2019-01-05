# WifiMouse
WifiMouse Android App

this code is a bit messy, it was my first large application developed completely by me. it has some bugs, one that is most annoying and apparent is the tap-to-click function does not work on some phones due to the touch slop sensitivity. to create the right feel for the touchpad remote i opted to use raw x & y input rather than wrangle with tap listeners. this works on older phones, but the high res newer ones seem to have issues. putting your finger down and lifting it up almost always counts as a touch move, newer phones classify a tap as a very small amount of movement. i tried to add code for this but it is still buggy. haven't fixed because it isn't broken on my phone and that touchpad remote should probably be entirely rewritten with touch/gesture listeners.

accepting pull requests if anyone wants to work on it.
