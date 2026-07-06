import type { PipelineStage, CanvasNode, CanvasEdge } from '@/types/pipeline'

const STAGE_WIDTH = 200
const STAGE_HEIGHT = 80
const PARALLEL_HEIGHT = 60
const GAP_X = 100
const GAP_Y = 50
const START_END_SIZE = 60

export function calculateLayout(stages: PipelineStage[]): { nodes: CanvasNode[], edges: CanvasEdge[] } {
  const nodes: CanvasNode[] = []
  const edges: CanvasEdge[] = []

  const startNode: CanvasNode = {
    id: 'start',
    type: 'start',
    data: null,
    position: { x: 50, y: 200, width: START_END_SIZE, height: START_END_SIZE }
  }
  nodes.push(startNode)

  let currentX = 50 + START_END_SIZE + GAP_X
  let prevNodeId = 'start'

  for (let i = 0; i < stages.length; i++) {
    const stage = stages[i]

    if (stage.type === 'parallel') {
      const parallelGroupId = `parallel_${stage.id}`
      const branchCount = stage.branches.length
      const totalHeight = branchCount * PARALLEL_HEIGHT + (branchCount - 1) * GAP_Y + 40
      const startY = 200 - totalHeight / 2 + 40

      const groupNode: CanvasNode = {
        id: parallelGroupId,
        type: 'parallel-group',
        data: stage,
        position: { x: currentX, y: startY - 20, width: STAGE_WIDTH, height: totalHeight }
      }
      nodes.push(groupNode)

      for (let j = 0; j < branchCount; j++) {
        const branch = stage.branches[j]
        const branchNode: CanvasNode = {
          id: branch.id,
          type: 'stage',
          data: stage,
          position: {
            x: currentX + 20,
            y: startY + j * (PARALLEL_HEIGHT + GAP_Y),
            width: STAGE_WIDTH - 40,
            height: PARALLEL_HEIGHT
          },
          parentId: parallelGroupId
        }
        nodes.push(branchNode)
      }

      edges.push({
        id: `edge_${prevNodeId}_${parallelGroupId}`,
        source: prevNodeId,
        target: parallelGroupId,
        type: 'default'
      })

      prevNodeId = parallelGroupId
      currentX += STAGE_WIDTH + GAP_X
    } else {
      const stageNode: CanvasNode = {
        id: stage.id,
        type: 'stage',
        data: stage,
        position: { x: currentX, y: 200 - STAGE_HEIGHT / 2, width: STAGE_WIDTH, height: STAGE_HEIGHT }
      }
      nodes.push(stageNode)

      edges.push({
        id: `edge_${prevNodeId}_${stage.id}`,
        source: prevNodeId,
        target: stage.id,
        type: 'default'
      })

      prevNodeId = stage.id
      currentX += STAGE_WIDTH + GAP_X
    }
  }

  const endNode: CanvasNode = {
    id: 'end',
    type: 'end',
    data: null,
    position: { x: currentX, y: 200 - START_END_SIZE / 2, width: START_END_SIZE, height: START_END_SIZE }
  }
  nodes.push(endNode)

  edges.push({
    id: `edge_${prevNodeId}_end`,
    source: prevNodeId,
    target: 'end',
    type: 'default'
  })

  return { nodes, edges }
}

export function getStatusColor(status?: string): { bg: string; border: string; text: string } {
  switch (status) {
    case 'success':
      return { bg: '#52c41a', border: '#389e0d', text: '#fff' }
    case 'failed':
      return { bg: '#ff4d4f', border: '#cf1322', text: '#fff' }
    case 'running':
      return { bg: '#1890ff', border: '#096dd9', text: '#fff' }
    case 'aborted':
      return { bg: '#faad14', border: '#d48806', text: '#fff' }
    case 'unstable':
      return { bg: '#fa8c16', border: '#d46b08', text: '#fff' }
    case 'skipped':
      return { bg: '#d9d9d9', border: '#bfbfbf', text: '#666' }
    case 'pending':
    default:
      return { bg: '#f0f0f0', border: '#d9d9d9', text: '#666' }
  }
}

export function generateMockLogs(stageName: string, stepName: string, status: string): string[] {
  const timestamp = new Date().toISOString()
  const logs: string[] = [
    `[${timestamp}] Starting ${stageName} > ${stepName}`,
    `[${timestamp}] Loading configuration...`,
    `[${timestamp}] Environment prepared`,
  ]

  if (status === 'success') {
    logs.push(`[${timestamp}] Executing command: echo "Build started"`)
    logs.push(`[${timestamp}] Build started`)
    logs.push(`[${timestamp}] Process completed successfully`)
    logs.push(`[${timestamp}] ${stageName} > ${stepName} completed`)
  } else if (status === 'failed') {
    logs.push(`[${timestamp}] Executing command: exit 1`)
    logs.push(`[${timestamp}] ERROR: Command failed with exit code 1`)
    logs.push(`[${timestamp}] ${stageName} > ${stepName} failed`)
  } else if (status === 'running') {
    logs.push(`[${timestamp}] Executing command: sleep 30`)
    logs.push(`[${timestamp}] Still running...`)
  }

  return logs
}
