import Image
import sys
imleft = Image.open(sys.argv[1])
imright = Image.open(sys.argv[2])
print sys.argv[1], imleft.getbbox()
print sys.argv[2], imright.getbbox()

imleftsplit = imleft.split()
imrightsplit = imright.split()
imana = Image.merge("RGB", (imleftsplit[0], imrightsplit[1], imrightsplit[2]))
imana.save("a.png")



