package com.benjaminhoogterp;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Random;
import java.util.Set;

@SupportedAnnotationTypes("com.benjaminhoogterp.*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
    private Trees trees;
    private TreeMaker make;
    private Name.Table names;
    private Symtab syms;
    private Types types;
    private Resolve rs;

    private Parser parser;
    private Modules modules;
    private JavacElements elements;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        trees = Trees.instance(env);
        Context context = ((JavacProcessingEnvironment) env).getContext();
        make = TreeMaker.instance(context);
        names = Names.instance(context).table;
        syms = Symtab.instance(context);
        types = Types.instance(context);
        rs = Resolve.instance(context);

        modules = Modules.instance(context);

        elements = JavacElements.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> { // Iterate over annotations that this processor supports
            roundEnv.getElementsAnnotatedWith(annotation).forEach(element -> {
                if (annotation.toString().contains("WrapMethod")) {
                    WrapMethod anno = element.getAnnotation(WrapMethod.class);
                    if (anno != null) doWrapMethod(element);
                }
            });
        });
        return true;
    }

    private void doWrapMethod(Element element) {
        JCTree.JCMethodDecl decl = (JCTree.JCMethodDecl) trees.getTree(element);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getTree(decl.sym.owner);

        List<JCTree.JCAnnotation> wrapMethod = List.nil();
        for (JCTree.JCAnnotation anno : decl.mods.annotations) {
            if (anno.annotationType.toString().contains("WrapMethod"))
                wrapMethod = wrapMethod.append(anno);
        }

        decl.body = rewriteFunctionForTrace(decl, wrapMethod.get(0));
//        System.out.println(decl.body);
    }

    private JCTree.JCBlock rewriteFunctionForTrace(JCTree.JCMethodDecl decl, JCTree.JCAnnotation anno) {
        final String trueValue = "true";  // not the hardware store
        final String util = "com.benjaminhoogterp.MyUtil";
        final String create = "createResource()";
        final String returnVarName = "return_";
        final String resourceName = "resource";
        final String resourceType = "com.benjaminhoogterp.MyResource";
        final String recordWrapParams = "recordTraceParams(java.lang.Object...)";
        final String recordReturnValue = "recordReturnValue(java.lang.Object)";

        // NOTE: This prevents compiler assertion errors on the `Bits.incl` method.
        //       Without this, `.sym` and `.sym.adr` are left at zero and the assertion fails.
        //       Apparently:  "But the newVar() is called by visitVarDef() only if tree.sym is trackable"
        //       https://stackoverflow.com/questions/46874126/java-lang-assertionerror-thrown-by-compiler-when-adding-generated-method-with-pa
        make.at(decl.pos);

        Attribute value = anno.attribute.member(names.fromString("recordParams"));
        boolean annoDoParams = value == null || value.toString().equals(trueValue);
        value = anno.attribute.member(names.fromString("recordResult"));
        boolean annoDoResut = value == null || value.toString().equals(trueValue);
        JCTree.JCBlock oldBody = decl.body;
        JCTree.JCExpression initializer = make.App(make.QualIdent(findSymbol(util, create)));

        JCTree resource = make.VarDef(
                make.Modifiers(0L),
                names.fromString(resourceName),
                make.Type(findSymbol(resourceType).type),
                initializer
        );

        List<JCTree.JCExpression> params = List.nil();
        params = params.append(make.Ident(names.fromString(resourceName)));
        for (JCTree.JCVariableDecl p : decl.getParameters()) {
            params = params.append(make.Ident(p));
        }

        // If enabled, calls the utility with all the function arguments.
        JCTree.JCStatement recordParams = make.Exec(make.App(make.QualIdent(findSymbol(util, recordWrapParams)), params));

        List<JCTree.JCStatement> statements = List.nil();
        if (annoDoParams) statements = statements.append(recordParams);
        for (JCTree.JCStatement stmt : oldBody.getStatements()) {

            if (!(stmt instanceof JCTree.JCReturn)) {
                statements = statements.append(stmt);
            } else {
                String actualReturnVar = returnVarName + new Random().nextInt(10000);
                JCTree.JCReturn ret = (JCTree.JCReturn) stmt;
                JCTree.JCExpression exp = ret.getExpression();

                JCTree.JCVariableDecl origStmtToVar = make.VarDef(make.Modifiers(0L), names.fromString(actualReturnVar), make.Type(decl.getReturnType().type), exp);
                statements = statements.append(origStmtToVar);

                // Calls a static method on the util with the value, which allows logging or any other process
                JCTree.JCStatement recordResult = make.Exec(make.App(make.QualIdent(findSymbol(util, recordReturnValue)), List.of(make.Ident(names.fromString(actualReturnVar)))));
                if (annoDoResut) statements = statements.append(recordResult);

                statements = statements.append(make.Return(make.Ident(names.fromString(actualReturnVar))));
            }
        }

        JCTree.JCBlock tryContents = make.Block(0L, statements);

        JCTree.JCStatement tryBlock = make.Try(List.of(resource), tryContents, List.nil(), null);
        JCTree.JCStatement wrapper = make.Block(0L, List.of(tryBlock));

// This is useful to view the outputted code as the JCStatement serializes to the code itself.
//        System.out.print("----------------\nWRAPPER:\n" + wrapper);
        return make.Block(0L, List.of(wrapper));
    }

    private Symbol findSymbol(String className) {
        Symbol classSymbol = elements.getTypeElement(className);
        if (classSymbol == null) throw new IllegalStateException("findSymbol: couldn't find symbol " + className);
        return classSymbol;
    }

    private Symbol findSymbol(String className, String symbolToString) {
        Symbol classSymbol = findSymbol(className);

        for (Symbol symbol : classSymbol.getEnclosedElements()) {
            if (symbolToString.equals(symbol.toString())) return symbol;
        }

        throw new IllegalStateException("findSymbol: couldn't find symbol " + className + "." + symbolToString);
    }
}
