import java.util.ArrayList;

import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class MiniGoPrintListener extends MiniGoBaseListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<>();
	private int localVar = 0;
	private int publicVar = 0;
	private int localArray = 0;
	private int publicArray = 0;
	private int depth = -1;
	private String blank = "           ";
	private String blankn = "\n           ";
	private ArrayList<Var> localVarL = new ArrayList<>();
	private ArrayList<Var> publicVarL = new ArrayList<>();
	private int level = 1;
	private int publicOffset = 0;
	private int localOffset = 1;

	// program : decl+ ;
	@Override
	public void exitProgram(MiniGoParser.ProgramContext ctx) {
		String str = "";
		for (MiniGoParser.DeclContext d : ctx.decl()) {
			str += this.newTexts.get(d);
			if (this.newTexts.get(d) == this.newTexts.get(d.fun_decl())) {
				str += "           ret\n" + "           end\n";
			}
		}

		str += this.blank + "bgn " + this.publicVar + "\n" + this.blank + "ldp" + this.blankn + "call main"
				+ this.blankn + "end\n";

		System.out.print(str);
	}

	@Override
	public void exitDecl(MiniGoParser.DeclContext ctx) {
		String str = "";
		if (ctx.getChild(0) == ctx.var_decl()) // decl : var_decl
		{
			str = this.newTexts.get(ctx.var_decl());
		} else if (ctx.getChild(0) == ctx.fun_decl()) // | fun_decl ;
		{
			str = this.newTexts.get(ctx.fun_decl());
		}
		this.newTexts.put(ctx, str);
	}

	@Override
	// 전역 변수 선언
	public void exitVar_decl(MiniGoParser.Var_declContext ctx) {
		String str = "";
		String IDENT = ctx.getChild(1).getText();
		this.publicVar++;
		if (ctx.getChildCount() == 3) { // VAR IDENT type_spec
			this.publicVarL.add(new Var(IDENT, this.level + " " + (++this.publicVar), false));
			str += this.blank + "sym " + this.level + " " + (++this.publicOffset) + " 1\n";
		} else if (ctx.getChildCount() == 5) { // VAR IDENT ',' IDENT type_spec
			this.publicVarL.add(new Var(IDENT, this.level + " " + (++this.publicVar), false));
			str += this.blank + "sym 1 " + this.publicVar + " 1\n";
			IDENT = ctx.getChild(3).getText();
			this.publicVarL.add(new Var(IDENT, this.level + " " + (++this.publicVar), false));
			str += this.blank + "sym 1 " + this.publicVar + " 1\n";
		} else if (ctx.getChildCount() == 6) { // VAR IDENT '[' LITERAL ']' type_spec ;
			this.publicArray = Integer.parseInt(ctx.LITERAL().getText());
			this.publicVarL.add(new Var(IDENT, this.level + " " + (++this.publicVar), true));
			str += this.blank + "sym " + this.level + " " + this.publicOffset + " " + this.publicArray + "\n";
			this.publicVar += this.publicArray;
		}

		this.newTexts.put(ctx, str);
	}

	@Override
	public void enterFun_decl(MiniGoParser.Fun_declContext ctx) {
		// block_level++;
		// have_return_stmt = false;
		super.enterFun_decl(ctx);
	}

	@Override
	public void exitFun_decl(MiniGoParser.Fun_declContext ctx) {
		String str = "";
		String _blank = "";
		String IDENT = ctx.getChild(1).getText();
		String compound_stmt = newTexts.get(ctx.compound_stmt());
		this.level++;
		for (int i = 0; i < 11 - IDENT.length(); i++) {
			_blank += " ";
		}

		str += IDENT + _blank + "proc " + (this.localVar + this.localArray) + " " + this.level + " 2\n";

		for (int i = 0; i < this.localVar; i++) {
			str += blank + "sym " + (this.level) + " " + this.localOffset++ + " 1\n";
		}

		str += compound_stmt;
		///////////////////////////////////////////////
		this.level--;
		this.localVar = 0;
		this.localArray = 0;

		newTexts.put(ctx, str);
	}

	////////////////////////////////////
	public void exitParams(MiniGoParser.ParamsContext ctx) {
		String str = "";

		if (ctx.getChildCount() == 0) {
			// str += ctx.getChild(0).getText();
		} else {
			for (MiniGoParser.ParamContext p : ctx.param()) {
				str += p.getText();
			}
		}
		newTexts.put(ctx, str);
	}

	@Override
	public void exitParam(MiniGoParser.ParamContext ctx) {
		String str = "";
		Var temp = new Var(ctx.getChild(0).getText(), this.level + " " + (++this.localVar), false);
		temp.isParam = true;
		if (ctx.getChildCount() == 4)
			temp.isArgs = true;
		this.localVarL.add(temp);
		newTexts.put(ctx, str); /////////////////////////
	}

	@Override
	public void exitStmt(MiniGoParser.StmtContext ctx) {
		String str = "";
		if (ctx.getChild(0) == ctx.expr_stmt())
			str += newTexts.get(ctx.expr_stmt());
		else if (ctx.getChild(0) == ctx.compound_stmt())
			str += newTexts.get(ctx.compound_stmt());
		else if (ctx.getChild(0) == ctx.assign_stmt())
			str += newTexts.get(ctx.assign_stmt());
		else if (ctx.getChild(0) == ctx.if_stmt())
			str += newTexts.get(ctx.if_stmt());
		else if (ctx.getChild(0) == ctx.for_stmt())
			str += newTexts.get(ctx.for_stmt());
		else if (ctx.getChild(0) == ctx.return_stmt())
			str += newTexts.get(ctx.return_stmt());
		newTexts.put(ctx, str);
	}

	@Override
	public void exitExpr_stmt(MiniGoParser.Expr_stmtContext ctx) {
		newTexts.put(ctx, newTexts.get(ctx.expr()));
	}

	@Override
	public void exitAssign_stmt(MiniGoParser.Assign_stmtContext ctx) {
		String str = "";
		Var temp;

		if (ctx.getChildCount() == 9) { // VAR IDENT ',' IDENT type_spec '=' LITERAL ',' LITERAL
			temp = new Var(ctx.getChild(1).getText(), this.level + " " + (++this.localVar), false);
			temp.isAssign = true;
			this.localVarL.add(temp);
			str += this.blank + "ldc " + ctx.getChild(6).getText() + "\n" + blank + "str " + temp.num + "\n";
			temp = new Var(ctx.getChild(3).getText(), this.level + " " + (++this.localVar), false);
			temp.isAssign = true;
			this.localVarL.add(temp);
			str += blank + "ldc " + ctx.getChild(8).getText() + "\n" + blank + "str " + temp.num + "\n";
		} else if (ctx.getChildCount() == 5) { // VAR IDENT type_spec '=' expr
			temp = new Var(ctx.IDENT(0).getText(), this.level + " " + (++this.localVar), false);
			temp.isAssign = true;
			this.localVarL.add(temp);
			str += this.newTexts.get(ctx.expr(0)) + blank + "str " + temp.num + "\n";
		} else if (ctx.getChildCount() == 4) { // IDENT type_spec '=' expr
			temp = this.lookUpVar(ctx.IDENT(0).getText());
			str += this.newTexts.get(ctx.expr(0)) + blank + "str " + temp.num + "\n";
		} else if (ctx.getChildCount() == 6) { // IDENT '[' expr ']' '=' expr ;
			temp = this.lookUpVar(ctx.IDENT(0).getText());
			str += this.newTexts.get(ctx.expr(0));
			if (temp.isArgs)
				str += blank + "lod " + temp.num + "\n";
			else
				str += blank + "lda " + temp.num + "\n";
			str += blank + "add\n" + newTexts.get(ctx.expr(1)) + blank + "sti\n";
		}
		this.newTexts.put(ctx, str);
	}

	@Override
	public void exitCompound_stmt(MiniGoParser.Compound_stmtContext ctx) {
		String str = "";

		for (MiniGoParser.Local_declContext l : ctx.local_decl()) {
			str += newTexts.get(l);
		}
		for (MiniGoParser.StmtContext s : ctx.stmt())
			str += newTexts.get(s);
		newTexts.put(ctx, str);
	}

	@Override
	public void exitIf_stmt(MiniGoParser.If_stmtContext ctx) {
		String str = "";
		str += newTexts.get(ctx.expr());

		str += blank + "fjp $$" + this.depth++ + "\n";

		if (ctx.getChildCount() == 3) { // IF expr compound_stmt
			str += newTexts.get(ctx.compound_stmt(0));
		}

		if (ctx.getChildCount() == 5) { // IF expr compound_stmt ELSE compound_stmt ;
			str += newTexts.get(ctx.compound_stmt(1));
		}

		str += "$$" + (--depth) + "        nop\n";

		newTexts.put(ctx, str);
	}

	@Override
	public void enterFor_stmt(MiniGoParser.For_stmtContext ctx) {
		depth++;
		super.enterFor_stmt(ctx);
	}

	@Override
	public void exitFor_stmt(MiniGoParser.For_stmtContext ctx) {
		// FOR expr compound_stmt;
		String str = "";
		str += "$$" + depth++ + "        nop\n";
		str += newTexts.get(ctx.expr());
		str += blank + "fjp $$" + depth + "\n";
		str += newTexts.get(ctx.compound_stmt());
		str += blank + "ujp $$" + (--depth) + "\n";
		str += "$$" + ++depth + "        nop\n";

		newTexts.put(ctx, str);
	}

	@Override
	public void exitReturn_stmt(MiniGoParser.Return_stmtContext ctx) {
		String str = "";
		if (ctx.getChildCount() == 4) { // RETURN expr ',' expr
			str += newTexts.get(ctx.expr(0)) + newTexts.get(ctx.expr(1)) + blank + "retv\n";
		} else if (ctx.getChildCount() == 2) { // RETURN expr
			str += newTexts.get(ctx.expr(0)) + blank + "retv\n";
		} else if (ctx.getChildCount() == 1) { // RETURN ;
			str += blank + "ret\n";
		}

		newTexts.put(ctx, str);
	}

	@Override
	public void exitLocal_decl(MiniGoParser.Local_declContext ctx) {
		if (ctx.getChildCount() == 3) { // VAR IDENT type_spec
			this.localVarL.add(new Var(ctx.getChild(1).getText(), "2 " + (++this.localVar), false));
		} else if (ctx.getChildCount() == 6) { // VAR IDENT '[' LITERAL ']' type_spec;
			this.localVarL.add(new Var(ctx.getChild(1).getText(), "2 " + (++this.localVar), true));
			this.localArray = Integer.parseInt(ctx.LITERAL().getText());
		}
		newTexts.put(ctx, "");
	}

	@Override
	public void exitExpr(MiniGoParser.ExprContext ctx) {
		String str = "";

		if (ctx.getChildCount() == 1) {
			if (ctx.LITERAL(0) != null) // LITERAL
				str += blank + "ldc " + ctx.LITERAL(0).getText() + "\n";
			else { // IDENT
				Var temp = this.lookUpVar(ctx.IDENT().getText());
				if (temp.isArray)
					str += blank + "lda " + temp.num + "\n";
				else
					str += blank + "lod " + temp.num + "\n";
			}
		} else if (ctx.getChildCount() == 2) { // op expr
			str += newTexts.get(ctx.expr(0));

			Var temp = this.lookUpVar(ctx.getChild(1).getText());

			if (ctx.getChild(0).getText().equals("-"))
				str += blank + "neg\n";
			else if (ctx.getChild(0).getText().equals("+"))
				str += blank + "pos\n";
			else if (ctx.getChild(0).getText().equals("--")) {
				str += blank + "dec\n" + blank + "str " + temp.num + "\n";
			} else if (ctx.getChild(0).getText().equals("++")) {
				str += blank + "inc\n" + blank + "str " + temp.num + "\n";
			} else if (ctx.getChild(0).getText().equals("!"))
				str += blank + "notop\n";
		} else if (ctx.getChildCount() == 3) {
			if (ctx.getChild(0).getText().equals("(") && ctx.getChild(2).getText().equals(")")) {
				// '(' expr ')'
				str += newTexts.get(ctx.expr(0));
			} else if (ctx.getChild(1).getText().equals(",")) {
				// LITERAL , LITERAL
				str += blank + "ldc " + ctx.LITERAL(0).getText() + "\n" + blank + "ldc " + ctx.LITERAL(1).getText()
						+ "\n";
			} else if (ctx.getChild(1).getText().equals("=")) {
				// IDENT = expr
				Var temp = this.lookUpVar(ctx.getChild(0).getText());
				str += newTexts.get(ctx.expr(0)) + blank + "str " + temp.num + "\n";
			} else {
				if (ctx.expr(0).IDENT() != null) {
					Var temp = this.lookUpVar(ctx.getChild(0).getText());
					str += blank + "lod " + temp.num + "\n";
				} else {// ctx.expr(0) == ctx.LITERAL()
					str += blank + "ldc " + ctx.getChild(0).getText() + "\n";
				}
				if (ctx.expr(1).IDENT() != null) {
					Var temp = this.lookUpVar(ctx.getChild(2).getText());
					str += blank + "lod " + temp.num + "\n";
				} else {// ctx.expr(0) == ctx.LITERAL()
					str += blank + "ldc " + ctx.getChild(2).getText() + "\n";
				}
				if (ctx.getChild(1).getText().equals("*")) {
					// expr * expr
					str += blank + "mult\n";
				} else if (ctx.getChild(1).getText().equals("/")) {
					// expr / expr
					str += blank + "div\n";
				} else if (ctx.getChild(1).getText().equals("%")) {
					// expr % expr
					str += blank + "mod\n";
				} else if (ctx.getChild(1).getText().equals("+")) {
					// expr + expr
					str += blank + "add\n";
				} else if (ctx.getChild(1).getText().equals("-")) {
					// expr - expr
					str += blank + "sub\n";
				} else if (ctx.getChild(1).getText().equals("==")) {
					// expr == expr
					str += blank + "eq\n";
				} else if (ctx.getChild(1).getText().equals("!=")) {
					// expr != expr
					str += blank + "ne\n";
				} else if (ctx.getChild(1).getText().equals("<=")) {
					// expr <= expr
					str += blank + "le\n";
				} else if (ctx.getChild(1).getText().equals("<")) {
					// expr < expr
					str += blank + "lt\n";
				} else if (ctx.getChild(1).getText().equals(">=")) {
					// expr >= expr
					str += blank + "ge\n";
				} else if (ctx.getChild(1).getText().equals(">")) {
					// expr > expr
					str += blank + "gt\n";
				} else if (ctx.getChild(1).getText().equals("&&")) {
					// expr && expr
					str += blank + "and\n";
				} else if (ctx.getChild(1).getText().equals("||")) {
					// expr|| expr
					str += blank + "or\n";
				}
			}
		} else if (ctx.getChildCount() == 4) {
			if (ctx.getChild(1).getText().equals("[")) {
				// IDENT '[' expr ']'
				Var temp = this.lookUpVar(ctx.IDENT().getText());
				str += newTexts.get(ctx.expr(0));
				if (!temp.isParam && !temp.isArgs) {
					str += blank + "lda ";
				} else {
					str += blank + "lod ";
				}
				str += temp.num + "\n" + blank + "add\n" + blank + "ldi\n";
			} else if (ctx.getChild(1).getText().equals("(")) {
				// IDENT '(' args ')'
				str += blank + "ldp\n" + newTexts.get(ctx.args()) + blank + "call " + ctx.getChild(0).getText() + "\n";
			}
		} else if (ctx.getChildCount() == 6) {
			if (ctx.FMT() != null) {
				// FMT '.' IDENT '(' args ')'
				str += blank + "ldp\n" + newTexts.get(ctx.args()) + blank + "call write\n";
			} else if (ctx.getChild(1).getText().equals("[")) {
				// IDENT '[' expr ']' '=' expr;
				Var temp = this.lookUpVar(ctx.IDENT().getText());
				str += newTexts.get(ctx.expr(0));
				if (temp.isArgs) {
					str += blank + "lod " + temp.num + "\n";
				} else {
					str += blank + "lda " + temp.num + "\n";
				}
				str += blank + "add\n" + newTexts.get(ctx.expr(1)) + blank + "sti\n";
			}
		}
		newTexts.put(ctx, str);
	}

	@Override
	public void exitArgs(MiniGoParser.ArgsContext ctx) {
		String str = "";
		for (MiniGoParser.ExprContext e : ctx.expr())
			str += newTexts.get(e);
		newTexts.put(ctx, str);
	}

	class Var {
		String IDENT;
		String num;
		boolean isArray = false;
		boolean isParam = false;
		boolean isArgs = false;
		boolean isAssign = false;

		public Var(String IDENT, String num, boolean isArray) {
			this.IDENT = IDENT;
			this.num = num;
			this.isArray = isArray;
		}
	}

	private Var lookUpPublicVar(String str) {
		for (Var v : publicVarL) {
			if (v.IDENT.equals(str))
				return v;
		}
		return null;
	}

	private Var lookUpLocalVar(String str) {
		for (Var v : localVarL) {
			if (v.IDENT.equals(str))
				return v;
		}
		return null;
	}

	private Var lookUpVar(String text) {
		Var var;
		if (lookUpLocalVar(text) != null)
			var = lookUpLocalVar(text);
		else
			var = lookUpPublicVar(text);
		return var;
	}
}
