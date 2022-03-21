# Overlay 

This module contains a 'stub' for the overlay. By default, it overlays no resources and is built
into the Magisk module. This allows it to then be installed on top of by PLM, and overlay resources
in the Pixel Launcher. 

Any changes to this module will be built into the module, but will not persist in builds made in
the app. 

See the root-level gradle file for the script responsible for building it. 