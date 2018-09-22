package org.openzen.zencode.shared;

public enum CompileExceptionCode {
	UNEXPECTED_TOKEN,
	IMPORT_NOT_FOUND,
	NO_OUTER_BECAUSE_NOT_INNER,
	NO_OUTER_BECAUSE_STATIC,
	NO_OUTER_BECAUSE_OUTSIDE_TYPE,
	TYPE_ARGUMENTS_INVALID_NUMBER,
	TYPE_ARGUMENTS_NOT_INFERRABLE,
	USING_STATIC_ON_INSTANCE,
	CANNOT_ASSIGN,
	UNAVAILABLE_IN_CLOSURE,
	USING_PACKAGE_AS_EXPRESSION,
	USING_PACKAGE_AS_CALL_TARGET,
	USING_TYPE_AS_EXPRESSION,
	MEMBER_NO_SETTER,
	MEMBER_NO_GETTER,
	MEMBER_NOT_STATIC,
	MEMBER_IS_FINAL,
	MEMBER_DUPLICATE,
	CALL_AMBIGUOUS,
	CALL_NO_VALID_METHOD,
	ENUM_VALUE_DUPLICATE,
	INVALID_CAST,
	NO_SUCH_INNER_TYPE,
	NO_DOLLAR_HERE,
	UNSUPPORTED_XML_EXPRESSIONS,
	UNSUPPORTED_NAMED_ARGUMENTS,
	TYPE_CANNOT_UNITE,
	BRACKET_MULTIPLE_EXPRESSIONS,
	SUPER_CALL_NO_SUPERCLASS,
	LAMBDA_HEADER_INVALID,
	COALESCE_TARGET_NOT_OPTIONAL,
	MULTIPLE_MATCHING_HINTS,
	MISSING_MAP_KEY,
	NO_SUCH_MEMBER,
	USING_THIS_OUTSIDE_TYPE,
	USING_THIS_STATIC,
	UNDEFINED_VARIABLE,
	METHOD_BODY_REQUIRED,
	BREAK_OUTSIDE_LOOP,
	CONTINUE_OUTSIDE_LOOP,
	NO_SUCH_ITERATOR,
	NO_SUCH_TYPE,
	RETURN_VALUE_REQUIRED,
	RETURN_VALUE_VOID,
	INVALID_CONDITION,
	INTERNAL_ERROR,
	CANNOT_SET_FINAL_VARIABLE,
	MISSING_PARAMETER,
	STATEMENT_OUTSIDE_SWITCH_CASE,
	MISSING_VARIANT_CASEPARAMETERS,
	INVALID_SWITCH_CASE,
	TRY_CONVERT_OUTSIDE_FUNCTION,
	TRY_CONVERT_ILLEGAL_TARGET,
	TRY_RETHROW_NOT_A_RESULT,
	DIFFERENT_EXCEPTIONS,
	UNKNOWN_ANNOTATION,
	OVERRIDE_WITHOUT_BASE,
	OVERRIDE_AMBIGUOUS,
	OVERRIDE_CONSTRUCTOR,
	PRECOMPILE_FAILED,
	UNTYPED_EMPTY_ARRAY,
	UNTYPED_EMPTY_MAP,
	VAR_WITHOUT_TYPE_OR_INITIALIZER,
	NO_BRACKET_PARSER,
	INVALID_BRACKET_EXPRESSION,
	VARIANT_OPTION_NOT_AN_EXPRESSION,
	DUPLICATE_GLOBAL,
	CANNOT_INFER_RETURN_TYPE,
	INVALID_SUFFIX,
	NO_SUCH_MODULE,
	
	NO_SUCH_STORAGE_TYPE,
	INVALID_STORAGE_TYPE_ARGUMENTS
}
