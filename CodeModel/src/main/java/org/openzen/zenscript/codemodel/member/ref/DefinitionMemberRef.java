/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openzen.zenscript.codemodel.member.ref;

import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.FunctionHeader;
import org.openzen.zenscript.codemodel.annotations.MemberAnnotation;
import org.openzen.zenscript.codemodel.member.IDefinitionMember;

/**
 *
 * @author Hoofdgebruiker
 */
public interface DefinitionMemberRef {
	CodePosition getPosition();
	
	String describe();
	
	<T> T getTag(Class<T> type);
	
	DefinitionMemberRef getOverrides();
	
	FunctionHeader getHeader();
	
	MemberAnnotation[] getAnnotations();
	
	IDefinitionMember getTarget();
}
