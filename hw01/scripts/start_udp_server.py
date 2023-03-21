import subprocess

subprocess.call(['java', '-jar', 'pcd-hw01-1.0.jar',
	'--type', 'server',
	'--protocol', 'UDP',
	'--port', '8100',
	'--messageSize', '10240'])