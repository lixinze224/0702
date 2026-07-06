# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
npm install      # Install dependencies
npm run dev      # Start dev server (port 3000)
npm run build    # Build for production (runs type checking first)
npm run preview  # Preview production build
```

## Architecture Overview

### Data Flow

The application follows a unidirectional data flow managed by **Pinia store** (`src/stores/pipeline.ts`):

```
User Action → Store Action → State Change → Reactive UI Update
```

### Core State (PipelineStore)

The store manages:
- `pipelines[]` - List of all pipeline definitions
- `currentPipelineId` - Active pipeline
- `runs[]` - Historical execution records
- `isRunning` - Execution flag

### Key Types (`src/types/pipeline.ts`)

- **Pipeline** - Top-level structure with `stages[]`, `agent`, `environment[]`, `post`
- **PipelineStage** - Contains `branches[]` and `type` ('sequential' | 'parallel')
- **PipelineBranch** - Contains `steps[]` for parallel execution branches
- **PipelineStep** - Atomic action with `type` and `config`
- **PipelineRun** - Execution record with `stages[]` status tracking

### Step Types

16 step types defined in `StepType`: `sh`, `echo`, `git`, `archiveArtifacts`, `junit`, `input`, `timeout`, `retry`, `sleep`, `cleanWs`, `build`, `script`, `withCredentials`, `withEnv`, `dir`, `stash`, `unstash`, `custom`

### Jenkinsfile Conversion (`src/utils/jenkinsfile.ts`)

- `pipelineToJenkinsfile()` - Converts Pipeline to Jenkinsfile string
- `jenkinsfileToPipeline()` - Parses Jenkinsfile back to Pipeline (partial)

### Components

- **PipelineCanvas.vue** - Visual pipeline editor (center area)
- **StageEditor.vue** - Stage/step configuration dialog
- **LogViewer.vue** - Execution log display

## Development Notes

### Stage Structure Logic

- `type: 'sequential'` → `branches` has exactly 1 element
- `type: 'parallel'` → `branches` has multiple elements (one per parallel branch)

### Simulated Pipeline Execution

`store.runPipeline()` simulates execution with:
- 1.5-3.5 second delay per stage
- 90% success rate (random)
- Generates mock logs via `generateMockLogs()` from `layout.ts`
- On failure, subsequent stages marked `skipped`

### Adding New Step Types

1. Add to `StepType` in `types/pipeline.ts`
2. Add handling in `stepToJenkinsfile()` in `utils/jenkinsfile.ts`
3. Add form fields in `StageEditor.vue` if config needed