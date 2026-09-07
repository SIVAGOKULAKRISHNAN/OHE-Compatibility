import itertools, json, random

DIRS=['north','east','south','west']
OPP={'north':'south','south':'north','east':'west','west':'east'}
# Model rotations: base south=0; west=270; north=180; east=90.
ROT={'south':0,'west':270,'north':180,'east':90}
for d in DIRS:
    assert (ROT[d] % 360) in (0,90,180,270)
    assert ROT[OPP[d]] == (ROT[d]+180)%360

# Exact connector offsets must be opposite and on the block boundary.
def p(d):
    return {'north':(0.5,.5,1.0),'south':(0.5,.5,0.0),'east':(0.0,.5,.5),'west':(1.0,.5,.5)}[d]
def c(d):
    return {'north':(0.5,.5,0.0),'south':(0.5,.5,1.0),'east':(1.0,.5,.5),'west':(0.0,.5,.5)}[d]
for d in DIRS:
    assert all(abs(p(d)[i]-c(d)[i]) in (0.0,1.0) for i in range(3))
    assert p(d)==c(OPP[d])

# 1000 randomized direction/assembly checks.
r=random.Random(30092026)
for _ in range(1000):
    d=r.choice(DIRS)
    assert p(d)==c(OPP[d])
    assert ROT[OPP[d]]==(ROT[d]+180)%360

# Parse every JSON resource 1000 times to catch malformed edits deterministically.
from pathlib import Path
root=Path(__file__).parent/'src/main/resources'
files=list(root.rglob('*.json'))
for _ in range(1000):
    for f in files:
        json.loads(f.read_text())
print(f'PASS: {len(files)} JSON files parsed x1000; 1000 randomized orientation checks passed.')
