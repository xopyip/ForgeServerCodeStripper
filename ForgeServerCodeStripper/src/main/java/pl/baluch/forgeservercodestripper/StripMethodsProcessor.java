package pl.baluch.forgeservercodestripper;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;

@SupportedAnnotationTypes({"pl.baluch.forgeservercodestripper.StripeForClient"})
public final class StripMethodsProcessor extends AbstractProcessor {
	private JavacElements elementUtils;
	private Trees trees;
	
	@Override
	public void init(final ProcessingEnvironment procEnv) {
		super.init(procEnv);
		this.elementUtils = (JavacElements) procEnv.getElementUtils();
		JavacProcessingEnvironment jcEnv = null;
		if (procEnv instanceof JavacProcessingEnvironment) {
			jcEnv = (JavacProcessingEnvironment) procEnv;
		} else {
			try {
				Field f = procEnv.getClass().getDeclaredField("delegate");
				f.setAccessible(true);
				jcEnv = (JavacProcessingEnvironment) f.get(procEnv);
			} catch (Exception e) {
				LogUtils.Log(e);
			}
		}
		if (jcEnv!=null) {
			this.trees = Trees.instance(jcEnv);
		}
	}
	
	@Override public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}
	
	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(StripeForClient.class);
		for (Element field : elements) {
			StripeForClient annotation = field.getAnnotation(StripeForClient.class);
			ElementKind KIND = field.getKind();
			LogUtils.Log("StripMethods::", annotation, KIND);
			if(KIND == ElementKind.CLASS) {
				ArrayList<JCTree> defs;
				if (trees!=null) {
					TreePath treePath = trees.getPath(field);
					JCTree.JCCompilationUnit compileUnit = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();
					int imports = compileUnit.getImports().size();
					defs = new ArrayList<>(compileUnit.defs);
					for (int i = 0, len=defs.size(); i <= len && imports>0; i++) {
						JCTree obj = defs.get(i);
						if (obj instanceof JCTree.JCImport) {
							defs.remove(i); i--; len--;
							imports--;
						}
					}
					compileUnit.defs = List.from(defs);
				}
				JCTree.JCClassDecl laDcl = (JCTree.JCClassDecl) elementUtils.getTree(field);
				defs = new ArrayList<>(laDcl.defs);
				for (int i = defs.size()-1; i >= 0; i--) {
					JCTree member = defs.get(i);
					if (member instanceof JCTree.JCMethodDecl) {
						LogUtils.Log("StripMethods::", member);
						defs.remove(i);
					} else if (member instanceof JCTree.JCVariableDecl) {
						LogUtils.Log("StripVariables::", member);
						defs.remove(i);
					} else if (member instanceof JCTree.JCClassDecl) {
						LogUtils.Log("StripClasses::", member);
						defs.remove(i);
					}
				}
				laDcl.defs = List.from(defs);
			}
		}
		
		return true;
	}
}
