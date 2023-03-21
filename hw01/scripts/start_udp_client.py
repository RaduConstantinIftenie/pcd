import subprocess

subprocess.call(['java', '-jar', 'pcd-hw01-1.0.jar',
	'--type', 'client',
	'--protocol', 'UDP',
	'--port', '8100',
	'--serverAddress', '127.0.0.1',
	'--messageSize', '10240',
	'--filePath', 'c:\Down\FilesToBeTransfered\ideaIU-2022.3.3.win.zip'])