// Regole architetturali condivise per la verifica dei layer interni a un Bounded Context.

import { projectFiles } from 'archunit';

function layerPath(boundedContext: string, layer: string): string {
  return `src/${boundedContext}/${layer}/**`;
}

export function domainMustNotDependOnOuterLayers(boundedContext: string) {
  return projectFiles()
    .inFolder(layerPath(boundedContext, 'domain'))
    .shouldNot()
    .dependOnFiles()
    .inFolder(new RegExp(`src\\/${boundedContext}\\/(application|infrastructure|api)`));
}

export function applicationMustNotDependOnAdapters(boundedContext: string) {
  return projectFiles()
    .inFolder(layerPath(boundedContext, 'application'))
    .shouldNot()
    .dependOnFiles()
    .inFolder(new RegExp(`src\\/${boundedContext}\\/(infrastructure|api)`));
}

export function infrastructureMustNotDependOnApi(boundedContext: string) {
  return projectFiles()
    .inFolder(layerPath(boundedContext, 'infrastructure'))
    .shouldNot()
    .dependOnFiles()
    .inFolder(layerPath(boundedContext, 'api'));
}

export function apiMustNotDependOnInfrastructure(boundedContext: string) {
  return projectFiles()
    .inFolder(layerPath(boundedContext, 'api'))
    .shouldNot()
    .dependOnFiles()
    .inFolder(layerPath(boundedContext, 'infrastructure'));
}
