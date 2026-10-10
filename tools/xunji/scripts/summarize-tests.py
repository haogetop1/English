#!/usr/bin/env python3
"""Inspect real JUnit XML results; reject missing, empty, failed or skipped suites."""
from pathlib import Path
import json
import shutil
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
report={}
for module,source,minimum in [('core','core/build/test-results/test',35),('app','app/build/test-results/testDebugUnitTest',28)]:
    files=list((root/'android'/source).glob('TEST-*.xml'))
    totals=dict(tests=0,failures=0,errors=0,skipped=0)
    for file in files:
        suite=ET.parse(file).getroot()
        for key in totals:totals[key]+=int(suite.get(key,0))
    report[module]=totals
    assert totals['tests']>=minimum,f'{module}: no complete suite results'
    assert totals['failures']==totals['errors']==totals['skipped']==0,f'{module}: {totals}'
    target=root/'artifacts/test-results'/module
    target.mkdir(parents=True,exist_ok=True)
    for file in target.glob('TEST-*.xml'):file.unlink()
    for file in files:shutil.copy2(file,target/file.name)
(root/'artifacts/test-results/summary.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False))
