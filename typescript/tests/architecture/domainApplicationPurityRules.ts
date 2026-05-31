// Regole condivise per la verifica della purezza dei layer domain/application
// all'interno di un singolo Bounded Context.

import { projectFiles } from 'archunit';
import * as ts from 'typescript';

// FIXME: smell – domain/application should not depend on external libraries.
// ts-pattern is temporarily tolerated; it should be moved to outer layers or removed.
const ALLOWED_EXTERNAL_DEPENDENCIES = ['ts-pattern'];

function isPrimitiveType(typeNode: ts.TypeNode | undefined): boolean {
    if (typeNode === undefined) {
        return false;
    }

    if (
        typeNode.kind === ts.SyntaxKind.StringKeyword ||
        typeNode.kind === ts.SyntaxKind.NumberKeyword ||
        typeNode.kind === ts.SyntaxKind.BooleanKeyword ||
        typeNode.kind === ts.SyntaxKind.BigIntKeyword
    ) {
        return true;
    }

    if (ts.isTypeReferenceNode(typeNode)) {
        const name = typeNode.typeName.getText();
        return ['String', 'Number', 'Boolean', 'BigInt'].includes(name);
    }

    if (ts.isUnionTypeNode(typeNode)) {
        return typeNode.types.some(isPrimitiveType);
    }

    return false;
}

function parseSource(fileContent: string, filePath: string): ts.SourceFile {
    return ts.createSourceFile(filePath, fileContent, ts.ScriptTarget.Latest, true);
}

function isEnumLike(sourceFile: ts.SourceFile): boolean {
    let enumFound = false;
    ts.forEachChild(sourceFile, (node) => {
        if (node.kind === ts.SyntaxKind.EnumDeclaration) {
            enumFound = true;
        }
    });
    return enumFound;
}

function hasPrivateConstructor(sourceFile: ts.SourceFile): boolean {
    let found = false;
    const visit = (node: ts.Node): void => {
        if (ts.isConstructorDeclaration(node)) {
            const isPrivate =
                node.modifiers?.some((modifier) => modifier.kind === ts.SyntaxKind.PrivateKeyword) ?? false;
            if (isPrivate) {
                found = true;
            }
        }
        ts.forEachChild(node, visit);
    };
    visit(sourceFile);
    return found;
}

function isAsConstLike(sourceFile: ts.SourceFile): boolean {
    let found = false;
    const visit = (node: ts.Node): void => {
        if (
            ts.isAsExpression(node) &&
            ts.isTypeReferenceNode(node.type) &&
            ts.isIdentifier(node.type.typeName) &&
            node.type.typeName.text === 'const'
        ) {
            found = true;
        }
        ts.forEachChild(node, visit);
    };
    visit(sourceFile);
    return found;
}

function isExceptionClass(sourceFile: ts.SourceFile): boolean {
    let found = false;
    const visit = (node: ts.Node): void => {
        if (
            ts.isClassDeclaration(node) &&
            node.heritageClauses?.some((clause) =>
                clause.types.some((type) => type.expression.getText(sourceFile) === 'Error')
            )
        ) {
            found = true;
        }
        ts.forEachChild(node, visit);
    };
    visit(sourceFile);
    return found;
}

function targetFolder(boundedContext: string): RegExp {
    return new RegExp(`src\\/${boundedContext}\\/(domain|application)\\/`);
}

export function shouldOnlyDependOnProjectInternals(boundedContext: string) {
    return projectFiles()
        .inFolder(targetFolder(boundedContext))
        .should()
        .adhereTo(
            (file) => {
                const source = parseSource(file.content, file.path);
                let valid = true;
                ts.forEachChild(source, (node) => {
                    if (ts.isImportDeclaration(node)) {
                        const specifier = (node.moduleSpecifier as ts.StringLiteral).text;
                        const isRelative = specifier.startsWith('.');
                        const isInternalAlias = specifier.startsWith('@/');
                        const isBuiltIn = specifier.startsWith('node:');
                        const isAllowedExternal = ALLOWED_EXTERNAL_DEPENDENCIES.includes(specifier);
                        if (!isRelative && !isInternalAlias && !isBuiltIn && !isAllowedExternal) {
                            valid = false;
                        }
                    }
                });
                return valid;
            },
            `${boundedContext} domain/application files should only depend on project internals or built-in node: modules`
        );
}

export function shouldNotDeclarePrimitiveFields(boundedContext: string) {
    return projectFiles()
        .inFolder(targetFolder(boundedContext))
        .should()
        .adhereTo(
            (file) => {
                const source = parseSource(file.content, file.path);
                if (isEnumLike(source) || isAsConstLike(source)) {
                    return true;
                }

                let valid = true;
                const visit = (node: ts.Node): void => {
                    if (ts.isPropertyDeclaration(node)) {
                        const isStatic =
                            node.modifiers?.some((modifier) => modifier.kind === ts.SyntaxKind.StaticKeyword) ?? false;
                        if (!isStatic && isPrimitiveType(node.type)) {
                            valid = false;
                        }
                    }
                    ts.forEachChild(node, visit);
                };
                visit(source);
                return valid;
            },
            `${boundedContext} domain/application classes should not declare primitive/String/wrapper fields (static constants and enums excluded)`
        );
}

export function shouldNotUsePrimitiveParameters(boundedContext: string) {
    return projectFiles()
        .inFolder(targetFolder(boundedContext))
        .should()
        .adhereTo(
            (file) => {
                const source = parseSource(file.content, file.path);
                if (isEnumLike(source) || isAsConstLike(source) || hasPrivateConstructor(source) || isExceptionClass(source)) {
                    return true;
                }

                let valid = true;
                const visit = (node: ts.Node): void => {
                    if (ts.isParameter(node) && isPrimitiveType(node.type)) {
                        valid = false;
                    }
                    ts.forEachChild(node, visit);
                };
                visit(source);
                return valid;
            },
            `${boundedContext} domain/application methods/constructors should not use primitive/String/wrapper parameters (enums and enum-like classes excluded)`
        );
}
