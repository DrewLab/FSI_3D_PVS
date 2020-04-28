import os

cwd = os.getcwd()

# open the file once - store all the z values
f = open(cwd + '/Full_mesh.mphtxt', 'r')
while True:
	l1 =	f.readline() 
	if l1=="# Mesh point coordinates\n":
		break
zVals = []
while True:
	l1 =	f.readline().split() 
	if len(l1) == 0:
		break
	zVals.append(float(l1[2]))

# find max surface number

while True:
	l1 =	f.readline()
	if l1=="3 tri # type name\n":
		break

while True:
	l1 =	f.readline()
	if l1=="# Elements\n":
		break
nodeNums = []
while True:
	l1 =	f.readline().split() 
	if len(l1) == 0:
		break
	nodeNums.append([int(l) for l in l1])


while True:
	l1 =	f.readline()
	if l1=="# Geometric entity indices\n":
		break

surfNums = []


while True:
	l1 =	f.readline()
	if not l1:
		break
	l2 = l1.split()
	surfNums.append(int(l2[0]))

maxSurf = max(surfNums)
f.close()
print("Maximum surface number", maxSurf)

chgSurfNum = 8;


# open the file second time - copy all values till the surface numbers
f = open(cwd + '/Full_mesh.mphtxt', 'r')
f1 = open(cwd + '/Full_mesh2.mphtxt', 'w')

while True:
	l1 =	f.readline()
	f1.write(l1)
	if l1=="3 tri # type name\n":
		break

while True:
	l1 =	f.readline()
	f1.write(l1)
	if l1=="# Geometric entity indices\n":
		break

for i,n in enumerate(surfNums):
	if not n==chgSurfNum:
		f1.write(str(n) + "\n")
		continue
	newSurf = 0
	for j in range(3):
		if zVals[nodeNums[i][j]]< 220:
			newSurf=1
			break
	if newSurf==0:
		f1.write(str(n) + "\n")
	else:
		f1.write(str(maxSurf+1) + "\n")
f.close()
f1.close()

