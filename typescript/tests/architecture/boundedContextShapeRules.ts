// Regole di shape condivise per la verifica dei costrutti principali
// (Command e Policy) all'interno di un singolo Bounded Context.

import { projectFiles } from 'archunit';

function bcFolder(boundedContext: string, layer: string): string {
    return `src/${boundedContext}/${layer}/**`;
}

export function commandsMustImplementCommand(boundedContext: string) {
    return projectFiles()
        .inFolder(bcFolder(boundedContext, 'application/commands'))
        .should()
        .adhereTo(
            (file) => /Command\s*</.test(file.content),
            `files in ${bcFolder(boundedContext, 'application/commands')} should declare a Command type`
        );
}

export function policiesMustImplementPolicy(boundedContext: string) {
    return projectFiles()
        .inFolder(bcFolder(boundedContext, 'application/policies'))
        .should()
        .adhereTo(
            (file) => /implements\s+Policy\b/.test(file.content),
            `files in ${bcFolder(boundedContext, 'application/policies')} should implement Policy`
        );
}
