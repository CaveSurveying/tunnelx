import vtk
import sys

from math              import sqrt
from xml.sax.handler   import ContentHandler
from xml.sax.xmlreader import InputSource
from xml.sax           import make_parser

class CHandler(ContentHandler):
    def __init__(self):
        pass

    def FindZ(self, x0, y0, z0, x1, y1, z1, x2, y2):
        diffx10 = x1 - x0
        diffx20 = x2 - x0
        diffy10 = y1 - y0
        diffy20 = y2 - y0
        n       = diffx10 * diffx20 + diffy10 * diffy20
        d       = diffx10 * diffx10 + diffy10 * diffy10
        d       = sqrt(d)
        r       = n / (d * d)
        return (1.0 - r) * z0 + r * z1

    def startElement(self, name, attrs):
        output_function = getattr(self, "start_%s" % name, self.start_unkown_tag)
        output_function(name, attrs)

    def start_pyvtkarea(self, name, attrs):
        self.pyvtkareaIndex += 1
        if self.pyvtkareaIndex % 100 == 0:
            print "pyvtkareaIndex:%d" % self.pyvtkareaIndex
        self.ms      = vtk.vtkFloatArray()
        self.ms.SetNumberOfComponents(3)


    def start_tunnelxpyvtk(self, name, attrs):
        self.pyvtkareaIndex = -1
        self.appendF = vtk.vtkAppendPolyData()
        self.polygon = vtk.vtkPolygon()

    def start_path(self, name, attrs):
        self.Points = []
        for attrName in attrs.keys():
            if attrName == "zstart":
                self.z0 = float(attrs.get(attrName))
            elif attrName == "zend":
                self.z1 = float(attrs.get(attrName))
            elif attrName == "reversed":
                self.reversed = int(attrs.get(attrName))

    def start_pt(self, name, attrs):
        for attrName in attrs.keys():
            if attrName == "x":
                self.x = float(attrs.get(attrName))
            elif attrName == "y":
                self.y = float(attrs.get(attrName))
        self.Points.append([self.x,self.y])

    def start_unkown_tag(self, name, attrs):
        if name == None:
            print "Warning:no name given for tag"
        else:
            print "start of unknown tag:%s" % name
            for attrName in attrs.keys():
                print "Attribute -- Name: %s  Value: %s" % (attrName, attrs.get(attrName))

    def endElement(self, name):
        output_function = getattr(self, "end_%s" % name, self.end_unkown_tag)
        output_function(name)

    def end_pyvtkarea(self, name):
        polygon = self.polygon
        polygon.GetPoints().SetData(self.ms)
        n = polygon.GetPoints().GetNumberOfPoints()
        ids = polygon.GetPointIds()
        ids.SetNumberOfIds(n)
        for i in range(n):
            ids.SetId(i,i)
        ids = vtk.vtkIdList()
        polygon.Triangulate(ids)
        numberOfIds = ids.GetNumberOfIds()
        numberOfTriangles = numberOfIds / 3
        if ids.GetNumberOfIds() != numberOfTriangles * 3:
            print "id list should contain a multipul of 3 points"

        cellArray = vtk.vtkCellArray()
        for triIndex in range(numberOfTriangles):
            a = ids.GetId(3 * triIndex)
            b = ids.GetId(3 * triIndex + 1)
            c = ids.GetId(3 * triIndex + 2)
            cellArray.InsertNextCell(3)
            cellArray.InsertCellPoint(a)
            cellArray.InsertCellPoint(b)
            cellArray.InsertCellPoint(c)

        pD = vtk.vtkPolyData()
        points = vtk.vtkPoints()
        points.SetData(self.ms)
        pD.SetPoints(points)
        pD.SetPolys(cellArray)

        self.appendF.AddInput(pD)

    def end_tunnelxpyvtk(self, name):
        print "pyvtkareaIndex:%d" % self.pyvtkareaIndex

    def end_path(self, name):
        z0       = self.z0
        z1       = self.z1
        reversed = self.reversed
        points   = self.Points
        
        if reversed == 1:
            points.reverse()
            tmp = z0
            z0  = z1
            z1  = z0

        ptstart = points[0]
        ptend   = points[-1]
        
        x0 = ptstart[0]
        y0 = ptstart[1]
        
        x1 = ptend[0]
        y1 = ptend[1]
       
        if x0 == x1 and y0 == y1 and z0 != z1:
            print "the data is a bit nasty, but I shouldnt crash!"

        for point in points[:-1]:
            x2 = point[0]
            y2 = point[1]

            if z0 == z1:
                z2 = z0
            else:
                z2 = self.FindZ(x0, y0, z0, x1, y1, z1, x2, y2)

            self.ms.InsertNextTuple3(x2, y2, z2)

    def end_pt(self, name):
        pass

    def end_unkown_tag(self, name):
        if name == None:
            print "Warning:end of tag:no name given for tag"
        else:
            print "end of unknown tag:%s" % name


# the main part
xmlfilename = len(sys.argv) >= 2 and sys.argv[1] or 'forsimon.xml'

handler = CHandler()
xmlParser = make_parser()
xmlParser.setContentHandler(handler)
xmlParser.setErrorHandler(handler);
xmlParser.parse(InputSource(xmlfilename))

mapper = vtk.vtkPolyDataMapper()
mapper.SetInput(handler.appendF.GetOutput())
actor = vtk.vtkActor()
actor.SetMapper(mapper)

ren = vtk.vtkRenderer()
renWin = vtk.vtkRenderWindow()
renWin.AddRenderer(ren)
iren = vtk.vtkRenderWindowInteractor()
iren.SetRenderWindow(renWin)

ren.AddActor(actor)
ren.SetBackground(0.1, 0.2, 0.4)
renWin.SetSize(500, 500)

cam1 = ren.GetActiveCamera()
cam1.Elevation(-30)
cam1.Roll(-20)
ren.ResetCameraClippingRange()

iren.Initialize()
renWin.Render()
iren.Start()

