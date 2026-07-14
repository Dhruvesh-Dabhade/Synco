import xml.etree.ElementTree as ET
import glob
for f in glob.glob('/app/applet/app/build/test-results/testDebugUnitTest/TEST-*.xml'):
    tree = ET.parse(f)
    for testcase in tree.findall('.//testcase'):
        failure = testcase.find('failure')
        if failure is not None:
            print(f"FAILED: {testcase.attrib['name']}")
            print(failure.attrib['message'])
