import re

with open('src/utils/jenkinsfile.ts', 'r', encoding='utf-8') as f:
    content = f.read()

old_code = """    case 'javaImage':
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  def IMG_NAME = '${cfg.imageName || ''}'`)
      lines.push(`${prefix}  def dockerfileContent = '''${cfg.dockerfileContent || 'FROM openjdk:8'}'''`)
      lines.push(`${prefix}  def dockerBuildCmd = "echo \\\"\\${dockerfileContent}\\\" | docker build -t \\${IMG_NAME} -f - ."`)
      lines.push(`${prefix}  sh dockerBuildCmd`)
      lines.push(`${prefix}  def imgFullId = sh(returnStdout: true, script: "docker images --filter=reference=\\"${cfg.imageName}\" -q --no-trunc | head -n 1").trim()`)
      lines.push(`${prefix}  def imgShortId = imgFullId.replaceFirst('sha256:','').substring(0,12)`)
      lines.push(`${prefix}  echo "\${imgShortId}"`)
      lines.push(`${prefix}  env.IMG_SHORT_Id=imgShortId`)
      lines.push(`${prefix}  sh "docker save -o \${IMG_NAME}_\${imgShortId}.tar \${IMG_NAME}"`)
      lines.push(`${prefix}  env.IMG_WITH_ID="\${IMG_NAME}_\${imgShortId}"`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  sh "docker rmi \${env.IMG_SHORT_Id}"`)
      lines.push(`${prefix}}`)
      break"""

new_code = """    case 'javaImage':
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  def IMG_NAME = '${cfg.imageName || ''}'`)
      lines.push(`${prefix}  def dockerfileContent = '${cfg.dockerfileContent || 'FROM openjdk:8'}'`)
      lines.push(`${prefix}  sh '\\''cat > Dockerfile <<DOCKERFILE_EOF'\\''`)
      lines.push(`${prefix}  sh dockerfileContent`)
      lines.push(`${prefix}  sh '\\''DOCKERFILE_EOF'\\''`)
      lines.push(`${prefix}  sh '\\''docker build -t ${IMG_NAME} -f Dockerfile .'\\''`)
      lines.push(`${prefix}  def imgFullId = sh(returnStdout: true, script: "docker images --filter=reference=\\"${cfg.imageName}\" -q --no-trunc | head -n 1").trim()`)
      lines.push(`${prefix}  def imgShortId = imgFullId.replaceFirst('sha256:','').substring(0,12)`)
      lines.push(`${prefix}  echo "\${imgShortId}"`)
      lines.push(`${prefix}  env.IMG_SHORT_Id=imgShortId`)
      lines.push(`${prefix}  sh "docker save -o \${IMG_NAME}_\${imgShortId}.tar \${IMG_NAME}"`)
      lines.push(`${prefix}  env.IMG_WITH_ID="\${IMG_NAME}_\${imgShortId}"`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  sh "docker rmi \${env.IMG_SHORT_Id}"`)
      lines.push(`${prefix}}`)
      break"""

if old_code in content:
    content = content.replace(old_code, new_code)
    with open('src/utils/jenkinsfile.ts', 'w', encoding='utf-8') as f:
        f.write(content)
    print('Replacement successful')
else:
    print('Old code not found')
    # Debug: show what the actual content looks like around line 461
    lines = content.split('\n')
    print('Lines460-480:')
    for i, line in enumerate(lines[460:480], start=461):
        print(f'{i}: {repr(line)}')