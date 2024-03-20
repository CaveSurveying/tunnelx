# Main - Start here #

Welcome to TunnelX, a free Cave drawing system that depends on Survex and which does the 
same thing as Therion, except completely different.  
See **https://github.com/CaveSurveying/tunnelx** for updates and development.

If this is your first time using TunnelX, try opening the tutorials and double-clicking 
on the first one.


# Drawing #

Click the **Left Mouse Button** in the graphics pane whilst (optionally) holding down the *Shift* or *Control* keys.

* **Shift** + **Left Mouse** ends a path
* **Control** + **Left Mouse** starts or ends on a node
* **Shift+Control** + **Left Mouse** inserts a node on a selected path

The drop-down box in the top left of the window (or single letter buttons `'W' 'E' 'P' 'C' 'D' 'I' 'N' 'F'`) 
sets the type of the selected path, with `**'S'**` controlling the spline.  Buttons:

* **Delete** acts on any number of selected paths
* **Back** undo last click or button
* **Reflect** reverses direction of the selected path
* **Fuse** joins two selected paths allowing you to move paths and nodes

The **img** tab has a **Snap to grid** feature where you can change the grid spacing.
Centreline paths cannot be deleted without setting the menu **Action** -> **Allow Delete Centreline**.
The *Pitch Bound* and *Ceiling Bound* paths have a dash on one side to show the direction of the 
'whiskers' (always to the right according to the direction it was drawn).  Click **Reflect** to reverse it.
Connecting to the correct node among an overlapping set (some will be drawn as diamonds and pentagons) 
is possible by dragging away from the *Control+Left Mouse* selected node without releasing it in 
order to select the next one.


# Viewing #

Click and drag with the **Middle Mouse Button** to move the viewing position.  
Zoom with the **Scroll-wheel** or by holding the **Control-key** 
down before you click and drag the middle mouse.

The **View** menu contains the **Max** feature, and **Display** can turn on station names.  
Change the thickness of the lines using the **Stroke** buttons. 
Draw the final result with **Detail render**.

The **view** tab shows a second pane where you can store the current view using the **Copy** button.


# Selecting #

Click the **Right Mouse Button** on the desired path 
whilst (optionally) holding down the *Shift* or *Control* keys.  
Multiple clicks cycle through the overlapping paths.

* **Control** + **Right Mouse** selects (or deselects) multiple paths
* **Shift** + **Right Mouse** selects all paths for an area
	
The **Component** button selects all paths that connect to the selected path(s).  
Click **Component** again to select all paths to one side of the selected path(s).



# Centreline #

TunnelX is *Survex* based.  (see http://www.survex.com)
Either use **Import** -> **Import survex file** to load the centreline data, or 
do **File** -> **Open survex...** from the Main window.  
The centreline data can be previewed by selecting the dotted green (connective) 'S' to see the label text, 
or do **Import** -> **Wireframe view** to see the centreline in *Survex-Aven*.

If *Survex* is not found, **Import** -> **Use Survex** should be disabled, 
and TunnelX's computation (without loop closures) will be used.

Do **Import** -> **Import centreline** to load the geometry defined by the *Survex* data.

You can import a centreline as an elevation by including the line:<br />
;IMPORT_AS_ELEVATION 60<br />
somewhere in the survex file (where 60 is the angle of projection).  
All this does is loads it through a transformation which swaps the y axis for the z axis.


# Backgrounds #

Select the **img** tab for loading and moving the background image.
**Add image** adds a new image to the background - ideally this is a jpg file.

**Select image** requires you to select the rectangle outline of an image. This enables a
previously loaded image to show.  
Alternatively, use the drop-down box of visible background images and choose which you want.

Move the selected image into position by selecting a point on the image then drawing a single 
line path from this to the location on the survex centreline where that point belongs and 
clicking on the **Shift ground** button. 

Once you have one point that is roughly aligned between the image and the centreline rotate 
and resize the image. Do this by drawing a three point (two line) path - the first point is the
image location that is already correct (see stage above), the second point is a different point 
on the image and the third point is the location on the centreline where that second point belongs
then click **Shift ground** -- what happens here is that the first point is the centre of rotation 
while the second point is moved to the third point.

Always connect the corner of the rectangle outline of the image to part of the passage 
it depicts so that it stays in place when the passage moves due to the centreline shiting when loops are closed.


# Files (Main, smaller window) #

The Main (smaller) window shows the list of sketch files that you have opened.  
Double-click (or select it and do **Tunnel** -> **View sketch**) to work on a particular drawing and it will
take you to the drawing window.
Open an existing sketch file using **File** -> **Open sketch...**.
To make a new sketch use **File** -> **Open survex/topo** and navigate to your survex centreline and then, in the
drawing window which shows your survex, open your cave plan drawing that you want to copy (see Images section)

The sketch file name is *green* when it is loaded and up to date, and *red* if it needs to be saved.  

From the drawing window, use **File** -> **Save as...** to give it a different name.

To copy another sketch into the current sketch (while distorting to fit the centreline), 
select the other sketch in the Main window (just click ONCE on it; if you click twice it will open it in the 
drawing window which is not what you want) and do **Preview down sketch** to check that the sketch is 
importing in the correct location and that you can see how you will connect the two sketches. When you
are happy with this go **Import** -> **Import down sketch**.  Always preview the import using **Import Down Sketch** before downloading
for real because you cannot reverse the import down sketch.

Sketches can be downloaded from the internet by pasting their 'http://...' url into the file open dialog.


# Areas #

The **Update areas** button creates the areas of the sketch by finding the 
closed outlines of series of paths that are properly joined up at their nodes.  
Paths of type *Centreline* and *Connective* are ignored, but *Invisible* paths count.  

Preview the areas with **Display** -> **Stripe areas**.  
If areas do not appear, check for failed joins (eg two nodes nearby that should have been
fused into a single node) or unintended crossings near nodes, and check that the area 
itself does not self-intersect.

Disconnected features or rock pillars within an area must be joined to the outside walls
of your drawing with an *Invisible* path.  
To indicate a rock pillar (ie an area that is solid rock, not space) draw a *Connective* 
path into its centre from one of the nodes around it, click **Area signals** and select 
*rock* from the drop-down choices.  Then do **Update areas** to refresh.


# Z-depth #

The altitude of the paths and nodes are defined by the average of the nearest three centreline stations 
(by connectivity).  Compute this by clicking the **Update Node Z** button.  
The areas will be sorted by their average altitude and rendered in order.

Paths (and their areas) can be forced to a relative altitude by connecting them 
by a *Connective* line to a centreline node, clicking **Area signal**, 
selecting *zsetrelative* and changing the `0.0` to 
a different displacement.

Select a *Pitch Boundary* type path and do **Action** -> **Pitch Undercut** to 
create an  *Invisible* path beneath it connected by two *Connective* paths, 
which can be used for connecting a passage that breaks through the wall below the pitch.
This is necessary because you cannot connect three areas to one path.

# Show drawings for a subset of elevations  #

Select an area or centreline and do **Display** -> **Thin Z Selection** to restrict the visible drawing 
to a Z-range close to that which was selected. If this shows too little expand this visible area using 
**Display** -> **Widen Z Selection**.  

The altitudes of centreline stations can be shown using **Display** -> **Station Altitudes**.
Do **Colour** -> **Height** to fill in a colour spectrum of heights those visible in the 
graphics window at the time (zoom in to a small section of the cave to exaggerate the colour spread for that part).


# Symbols #

Symbols are placed on *Connective* paths.  They are always part of the area they point into from the node they join 
(though some of this path can go outside the area).

Click on **Add Symbols** and select the chosen symbol.  Some are single symbols (eg stalactite, straws), others are
directional (eg slope, stream), and the rest fill and area (eg puddle, boulders). In addition to the start node for
the connective path single symbols just need one more node showing where to place them. Directional symbols need two 
more nodes with the angle between these two nodes showing the symbol orienation. Area symbols need a single extra node
within the area that needs to be filled.

To render, first **Update Areas** to bind the symbols into the correct area, and then **Update Symbols** to lay them out.

The **subs** tab allows for setting the subset style for rendering the symbols to different scales.


# Symbol files #

The symbols directory contains all the basic symbols in the form of little sketches 
(eg a single boulder, one stream arrow, etc).  You can see and edit them them by doing 
**Tunnel** -> **Symbols list** from the MainBox.

The *fontcolours.xml* files contain the real work of defining what happens 
for each Subset Style.  For example, the baseSymbols250 style defines:

```
<symbolaut dname="stream" description="stream symbol" multiplicity="1" buttonaction="overwrite" area-interaction="allowed-outside" position="endpath" scale="fixed" orientation="fixed">
   <asymbol name="stream" picscale="0.5" orientation="nearaxis"/>
</symbolaut>
```

A symbol (usually a puddle) can be set to a solid fill colour using the parameter `symbolareafillcolour="#ff0000ff"`.

The *symbols* directory will be loaded from `[current-directory]/symbols` if it exists, or 
`[home-directory]/.tunnelx/symbols` (if in unix) or `[home-directory]/symbols` (if in windows), 
or finally `/usr/share/tunnelx/symbols/`.  If none of these exist, it will use the symbols directory 
that comes with the *.jar* file.


# Labels #

Labels are placed on *Connective* paths.  Click on **Write Text** and write the label in the text area, 
selecting the type of label from the drop down box.

The origin position is located at the first node of the path.  
The 3x3 choice matrix sets which corner or side of the box containing the text is placed on the origin.  
Fine positioning can be done by drawing a short path from the first node to a new
node where you want the label to be and clicking **Fuse**. This will move the location 
of the label to the new node (click Reflect if the label doesn't move as it depends which
of the path nodes the label is attached to).

Always connect one end of the connective path to the associated passage drawing so the label stays
in place when the passage is moved when the centreline distorts.

Use the **Arrow** selection to point at one end, and the **Box** to further highlight a label.


# Scale bars and N arrows  #

CARE! When using frames the scale bar needs to be explicitly told what scale to draw (because 
drawings at multiple scales can be shown in the same frame). To check the scale of any drawing
included in the frame, click on the connective line that you COPY to import it and look at the 
text box, check what sfscaledown="650.0" says - this is 1:650. Then adjust the scale bar lines
below, so for this scale change %0/1.0000%%v0/% (which draws at 1:1000) to %0/0.65%%v0/% then change 
the line type to vary the font and vertical size of the scale bar

Paste one of the following blocks of text into a label to produce a scale bar.  Simple version:

```
%10/1.0000%%whiterect%
;%10/%%blackrect%
;%10/%%whiterect%
;%10/%%blackrect%
;%10/%%whiterect%
%v1.0/1%
%10/%0m
;%10/%10m 
;%10/%20m
;%10/%30m
;%10/%40m
;%10/%50m
```

Complex scale bar (see below for editing this):

```
%0/1.0000%%v0/%
;%50/%%v1/%%whiterect%
%1/%%v0.5/%
;%1/%%v0.5/%%blackrect%
;%1/%
;%1/%%v0.5/%%blackrect%
;%1/%
;%5/%%v0.5/%%blackrect%
;%5/%%v1/%
;%5/%%v0.5/%%blackrect%
;%5/%%v1/%
;%5/%%v0.5/%%blackrect%
;%5/%%v1/%
;%5/%%v0.5/%%blackrect%
;%5/%%v1/%
;%5/%%v0.5/%%blackrect%
%1/%%v0.5/%%blackrect%
;%1/%
;%1/%%v0.5/%%blackrect%
;%1/%
;%1/%%v0.5/%%blackrect%
;%5/%
;%5/%%v0.5/%%blackrect%
;%5/%
;%5/%%v0.5/%%blackrect%
;%5/%
;%5/%%v0.5/%%blackrect%
;%5/%
;%5/%%v0.5/%%blackrect%
%v0.8/%
%4.5/%0m
;%5/%5m
;%10/%10m
;%10/%20m
;%10/%30m
;%10/%40m
;%10/%50m
```

North arrow:

If you use the text below the N is offset and it's tricky to get the size you want so it's often easier to omit
the N from the arrow code then just use a connective line - write text - N to add it in separately

```
N
%t1/0.1%%v0/%%h0/%
;%v3/%%t0/%%h2/%%whiterect%
%t1/%%v0/%%h0/%
;%v3/%%t0/%%h1/%%blackrect%
```

Left arrow:

```
%t2/0.1%%v0/%%h0/%
;%v0.5/%%t0/%%h2/%%whiterect%
%v0.5/0.1%
%v0.5/%%t0/%%h2/%
;%v0.5/%%t2/%%h0/%%blackrect%
```

Depth scale bar:

```
%10/3%%v50/%%blackrect%
;1800m
%10/%%v50.0/%%whiterect%
;1600m
%10/%%v50/%%blackrect%
;1500m
%10/%%v50.0/%%whiterect%
;1400m
%10/%%v50/%%blackrect%
;1300m
%10/%%v50.0/%%whiterect%
;1200m
%10/%%v50/%%blackrect%
;1100m
%10/%%v0/%
;1700m
```

The font size of the scale bars can be changed by altering the line type for the connective path

The **';'** at the start of the line means the block stays on the same row.  
(each new line is displaced down by the vertical height of the first block).
The code **'%X/Y%'** at the beginning of a block makes it have a width of *X/Y* metres, 
while **'%vX/Y'** sets its height.  
(If **'Y'** is left out, then it takes the previous value, so in the first example 
the 50m scale bar can be converted to a 500m scale bar by changing `1.0000` to `0.1` in the first line.)

The symbols **'%whiterect%'** and **'%blackrect%'** fill the block with an outline or a filled in rectangle.
Alternatively, place text here and use the blocks to define the cells of a table.

The top and bottom widths of a block can be set independently with **'%tX/Y'** for the top and **'%hX/Y'** for the bottom (the 'h' is optional) 
to produce triangles or parallelograms.

# Info Panel#

Use the **info** panel to find information about paths.  

**Searching** - Fill in the text box and click *search* to produce a list of labels the text 
appears in.  Click on the label to select the path.

**Making new paths** - Comma or space separated list in the same search box, then click on 
*New nodes* to add these nodes to the drawn path.


# Subsets #

The current subset can be selected from the tree view in the **subs** tab.  
Select a colour beneath the '`visiblesets`' and all the paths will turn grey.
Select a path (*Mouse Right*) or an area (*Shift+Mouse Right*) and 
click **Add to Subset** to include it in the subset. All areas and labels need to be added
for them to show in the correct subset colour. Label colours will only show after clicking **Detail Render**.

Do **Clear subset selection** to undo the subset selection and show all subsets again.  
The subsets of a selected path appears in the drop-down box at the bottom, 
which can be used for quick selection of an individual subset.

Named subsets can be made by making a *Connective* path, 
clicking **Area signal**, and choosing *frame* from the 
drop-down box.  Above the line `'</sketchframe>'` (the last line), insert:

* `<subsetattr uppersubset="blue" name="Secret Grotto"/>`


... then click **Copy**.  The tree view in the lower window will now have 
'`(Secret Grotto)`' beneath '`visiblesets`' -> '`blue`'.  
Select it to add paths and areas to the 'Secret Grotto' subset.  
The colour can be altered later by changing the value of '`uppersubset`'.
The colour set when '`name="default"`' applies to all remaining areas.


# Changes to centreline and loop closures

When the centreline gets extended and interlinked to other caves 
so that it changes due to loop closures it is necessary to reimport the centreline and then 
reimport the drawn sketches distorting them to this new centeline


# Frame #

Start a new empty sketch, and make a *Connective* path, 
Click **Area signal** and select *frame* from the drop-down box.  
Now click **Import** -> **Import paper** -> **Make A1** to create an *A1* size sheet of paper.

Draw a rectangle in it, make a path into it, and make it *frame* type too. Now we can add 
another sketch to it, apply **Max**, move it around into position, set its colours, and render it.

It's possible to render the same survey at two different scales in the same 
area with different subset styles overlaid on a aerial photo or bitmap of a map. 
See the Scale bar help tab for WARNING about scale bars for frames.

Put all the title box and other clobber in the frame, so as not to clutter the main survey with it.  
Use *subset style* `baseA3page` or similar to find a new set of fonts.

Anything that you want to add to the frame has to be BOTH added to the frame subset (this may be
paper_A0_page_1 or similar) AND to a font subset (eg frametitles)


Images can be placed inside areas (as well as other sketches) where they will be trimmed.  
This allows for background overlays of aerial imagery.

Multiple sketchs can appear in the same window, where the order is controlled by setting 
the *nodeconnzsetrelative* values.


# DistoX TOP files #

DistoX laser and compass devices that transmit their measurements to a Windows PDA via bluetooth 
saves its data into a binary **.top** file which contains the survey legs, plan drawing and 
elevation drawing in three separate sections.

You can open a **.top** file by doing 
**File** -> **Open survex...** from the Main window and selecting it.  
This will open both plan and elevation drawings into the same sketch and put the 
survey data into the label of the big green 'S'.

Unfortunately, this TOP file cannot be used natively in TunnelX because the lines tend to be 
disconnected and sketchy, so you will need to copy and paste the Survex data into its own text file and 
link it into the rest of the data by hand, and then render the drawings into two .png files by 
selecting the **subs** tab, then picking *plan_TOP* in the *_Unattributed_* folder 
before going to the **print** tab and rendering the image to a PNG file.  
(Don't forget to reset the dots/inch to a higher value for a better quality image.)
Do the same for the *elev_TOP* subset.  
It is important for the subset style to be *"pockettopo"* for the colours to come out.  
Now you can reload the whole survex file and 
add the rendering as the background image ready to be traced over.

To render the elevation centreline from a TOP survex file, open the .svx file from the Main window 
and click **Back** to get rid of the plan view.  Now you can do 
**Import** -> **Import Centreline Elev** to generate the extended elevation of this centreline.  
The legs that contain *flip_TOP* are oriented left to right, as well as 
any legs that have their tail visited first during the traversal from the starting point 
(which is either the first point in the survey, or the nearest fixed point).  

# Elevations #

*Provisional owing to user interface difficulties*

Drawings for cross sections and extended elevations are tied to a 
*Connective* path by all being in a subset of name "XC stationname" or "ELEV stationname1 stationname2".

To make a cross section, draw a *Connective* line from a node in one wall across the passage 
into a node in the other wall.  Then (with the path selected) do **Elevation** -> **XC Subset** to 
create the new subset (with a name of the form "XC something") and the axis of the cross section 
(as a disconnected centreline piece).  Move and fit this axis to the cross section (using **Fuse** and **Component**) 
and then draw around the cross section (connecting it to the axis).  
Note that an arrow pointer moves along the corresponding path cutting the passage for the purpose of lining up features 
between the plan and the cross section.

To make an extended elevation, draw a *Connective* line from a centreline node (or a node immediately connected to a centreline) 
to another such node, then do **Elevation** -> **Elevation Subset** to generate a long centreline path 
for use as the axis in the elevation drawing.

After the elevation has been drawn (with all the paths in the "ELEV" subset), the endpoint of the centreline axis 
can be moved (using **Fuse**) to stretch and fit the pieces together.

Use the **img** tab to see what is happening when the elevation/cross section drawing and corresponding 
place in the plan are too far apart to show in the same graphics area at a resonable scale.


# Printing #

The **print** tab enables output to PNG or JPG type images, which can then be printed 
using standard image handling software.  The printing area is either the bounding box for 
the currently selected subset (set through the **subs** tab), or the viewable graphics area.
Select the subset for the *A1 frame* to produce a consistent result.

The dimensions stated in *Real dimensions:* correspond to a  baseline scale of `1:1000`, 
so a 500m wide cave will be 50cm on the paper.  
Vary the pixel dimensions by changing the resolution in dots per inch (on this 1:1000 paper).

The directory for output and name of file are listed below.

Because the same sketch may appear as in different *subset styles*, 
a proper rendering may require the symbols to be layed out multiple times.  
Select *Full draw* (rather than the default of Quick draw) to enable this, or preview 
using one of the other modes.

Other options include output to *Gray scale* and *Transparent* colour to make the white 
areas alpha=0 for use in other graphics packages.

*Requires re-implementation:* If the centreline is in the right coordinate space, click on 
**Overlay** to render it and upload it to the cave map overlay automatically, for maximum 
speed of publication.

FRAMES - printing frames differs from printing normal sketches. Start by restarting Tunnel
then open the frame. Do NOT use **Copy** to bring in any of the separate sketches (as you would do
if you were working on the frame). Instead go to the **Print** tab, deselect Transparent, change
*Quick draw* to *Full draw* and you probably want dpi to be 200dpi or lower (remember to hit return
after you change the dpi so you can see the image dimensions change). Then do **File** -> **DrawImage**
This will create a png in the top level directory (you won't get asked where to save or filename).

# Command line #

To compile do:
`"C:\Program Files\Java\jdk1.6.0_26\bin\javac" -target 1.5 -Xlint:deprecation -d . src\*.java`
To run do:
`java -showversion -ea -Xmx1000M -cp . Tunnel.MainBox C:\\Users\\goatchurch\\tunneldata\\`

Other options:

* `--verbose`
* `--quiet`
* `--todenode`
* `--netconnection`
* `--makeimages` Automatically generates images from all the areas that have an areasignal of frame and subset "framestyle"
* `--printdir=`
* `--twotone` Forces a grey scale which is mapped to black and white pixels at a threshold of 65000

# Additional Resources #

A few other tutorials and resources exist for TunnelX which can be found through the following links:

* https://expo.survex.com/expofiles/tunnelwiki/wiki/pages/Tunnel.html ~**TunnelX Wiki**

* https://expo.survex.com/expofiles/documents/surveying/Tunnel_Guide.pdf ~**PDF Tutorial**

