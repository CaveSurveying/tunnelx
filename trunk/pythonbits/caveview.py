#!/usr/bin/env python

# This takes the file pyvtk.xml as its command line argument 
# which it then renders in 3D, as a pop-up book map.

import vtk
import math
import xml.sax
import os
import sys


# vtk stuff
class CaveVTK:
	def __init__(self):
		self.points = vtk.vtkPoints()
		self.lines = vtk.vtkCellArray()

		self.acareas = { }

	def AddArea(self, areapth, scol):
		# find the pair of pieces in the area for the colour
		if scol not in self.acareas:
			self.acareas[scol] = (vtk.vtkPoints(), vtk.vtkCellArray())

		apoints, atriangs = self.acareas[scol]

		# first make the point arrays
		dpoints = vtk.vtkPoints()
		dlines = vtk.vtkCellArray()
		dlines.InsertNextCell(len(areapth) + 1)
		j0 = apoints.GetNumberOfPoints()
		for pt in areapth:
			apoints.InsertNextPoint(pt[0], pt[1], pt[2])
			j = dpoints.InsertNextPoint(pt[0], pt[1], pt[2])
			dlines.InsertCellPoint(j)
		dlines.InsertCellPoint(0)

		dpolyd = vtk.vtkPolyData()
		dpolyd.SetPoints(dpoints)
		dpolyd.SetLines(dlines)

		delau = vtk.vtkDelaunay2D()
		delau.SetInput(dpolyd)
		delau.SetSource(dpolyd)
		delau.Update()

		# loop through the triangles
		dcells = delau.GetOutput().GetPolys()
		ddata = dcells.GetData()
		assert dcells.GetNumberOfCells() * 4 == ddata.GetNumberOfTuples()
		for it in range(dcells.GetNumberOfCells()):
			it4 = it * 4
			assert ddata.GetValue(it * 4) == 3
			t0 = ddata.GetValue(it * 4 + 1)
			t1 = ddata.GetValue(it * 4 + 2)
			t2 = ddata.GetValue(it * 4 + 3)

			# centre of the triangle
			cx = (areapth[t0][0] + areapth[t1][0] + areapth[t2][0]) / 3
			cy = (areapth[t0][1] + areapth[t1][1] + areapth[t2][1]) / 3

			# test containment
			cright = 0
			ptprev = areapth[-1]
			for pt in areapth:
				if (pt[1] < cy) != (ptprev[1] < cy):
					lam = (cy - ptprev[1]) / (pt[1] - ptprev[1])
					xr = ptprev[0] * (1.0 - lam) + pt[0] * lam
					if xr > cx:
						cright = 1 - cright
				ptprev = pt

			# if inside then add to cells
			if cright:
				atriangs.InsertNextCell(3)
				atriangs.InsertCellPoint(j0 + t0)
				atriangs.InsertCellPoint(j0 + t1)
				atriangs.InsertCellPoint(j0 + t2)


	def AddPath(self, pth, zstart, zend):
		self.lines.InsertNextCell(len(pth))
		x0, y0 = pth[0]
		xv, yv = pth[-1][0] - x0, pth[-1][1] - y0
		vsq = xv * xv + yv * yv

		i = 0
		for pt in pth:
			if vsq:
				lam = ((pt[0] - x0) * xv + (pt[1] - y0) * yv) / vsq
			else:
				lam = i / (len(pth) - 1.0)
			i += 1

			if lam <= 0.0:
				z = zstart
			elif lam >= 1.0:
				z = zend
			else:
				z = zstart * (1.0 - lam) + zend * lam
			j = self.points.InsertNextPoint(pt[0], pt[1], z)
			self.lines.InsertCellPoint(j)



	def ViewVTK(self):
		polydata = vtk.vtkPolyData()
		polydata.SetPoints(self.points)
		polydata.SetLines(self.lines)

		pdmap = vtk.vtkPolyDataMapper()
		pdmap.SetInput(polydata)
		pdact = vtk.vtkActor()
		pdact.SetMapper(pdmap)
		pdact.GetProperty().SetColor(0.3, 0.8, 1.0)

		ren = vtk.vtkRenderer()
		ren.AddActor(pdact)

		for scol in self.acareas:
			print scol
			apoints, atriangs = self.acareas[scol]

			dpd = vtk.vtkPolyData()
			dpd.SetPoints(apoints)
			dpd.SetPolys(atriangs)
			dmap = vtk.vtkPolyDataMapper()
			dmap.SetInput(dpd)
			dact = vtk.vtkActor()
			dact.SetMapper(dmap)
			dact.GetProperty().SetColor(eval("0x" + scol[1:3]) / 256.0, eval("0x" + scol[3:5]) / 256.0, eval("0x" + scol[5:7]) / 256.0)
			dact.GetProperty().SetAmbient(0.2)
			ren.AddActor(dact)

		renWin = vtk.vtkRenderWindow()
		renWin.AddRenderer(ren)
		renWin.SetSize(250, 250)

		iren = vtk.vtkRenderWindowInteractor()
		istyle = vtk.vtkInteractorStyleTerrain()
		iren.SetInteractorStyle(istyle)
		print "here", istyle, istyle.__str__()
		iren.SetRenderWindow(renWin)
		iren.Initialize()
		renWin.Render()
		print "there", iren.GetInteractorStyle(), iren.GetInteractorStyle().__str__
		iren.Start()


# cave loader
class CaveLoader(xml.sax.handler.ContentHandler):

	def __init__(self, lcavevtk, xfil):
		self.xdir = os.path.split(xfil)[0]
		self.cavevtk = lcavevtk
		self.pts = [ ]
		print "processing", xfil, "in directory", self.xdir

		parser = xml.sax.make_parser()
		parser.setContentHandler(self)
		parser.parse(xfil)

	def AddPathArea(self, pth, zstart, zend):
		x0, y0 = pth[0]
		xv, yv = pth[-1][0] - x0, pth[-1][1] - y0
		vsq = xv * xv + yv * yv

		i = 0
		for pt in pth:
			if vsq:
				lam = ((pt[0] - x0) * xv + (pt[1] - y0) * yv) / vsq
			else:
				lam = i / (len(pth) - 1.0)
			i += 1

			if lam <= 0.0:
				z = zstart
			elif lam >= 1.0:
				z = zend
			else:
				z = zstart * (1.0 - lam) + zend * lam

			if i:
				self.areapth.append((pt[0], pt[1], z))
			else:
				assert (not self.areapth) or self.areapth[-1] == (pt[0], pt[1], z)


	# the main xml incoming function
	def startElement(self, name, attr):
		if name == "pyvtkarea":
			self.scol = attr["colour"]
			self.areapth = [ ]

		elif name == "path":
			self.zstart = float(attr["zstart"])
			self.zend = float(attr["zend"])
			self.breversed = (attr["reversed"] == "1")
			self.pth = [ ]

		elif name == "pt":
			self.pth.append((float(attr["x"]), -float(attr["y"])))

	# the xml characters
	def characters(self, content):
		pass

	# the xml end tag
	def endElement(self, name):
		if name == "path":
			if self.breversed:
				self.pth.reverse()
				self.cavevtk.AddPath(self.pth, self.zend, self.zstart)
				self.AddPathArea(self.pth, self.zend, self.zstart)
			else:
				self.cavevtk.AddPath(self.pth, self.zstart, self.zend)
				self.AddPathArea(self.pth, self.zstart, self.zend)

		elif name == "pyvtkarea":
			assert self.areapth[0] == self.areapth[-1]
			self.areapth.pop()
			print len(self.areapth)
			self.cavevtk.AddArea(self.areapth, self.scol)



	def endDocument(self):
		pass #print "doc leng", len(self.prevflatb)



sfile = sys.argv[1]
cavevtk = CaveVTK()
CaveLoader(cavevtk, sfile)
cavevtk.ViewVTK()


