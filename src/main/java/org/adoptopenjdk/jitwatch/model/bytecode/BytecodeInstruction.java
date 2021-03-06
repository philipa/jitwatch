/*
 * Copyright (c) 2013, 2014 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.model.bytecode;

import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.C_COLON;
import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.C_COMMA;
import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.C_OPEN_BRACE;
import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.C_SPACE;
import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.S_BYTECODE_INTERFACEMETHOD_COMMENT;
import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.S_BYTECODE_METHOD_COMMENT;
import static org.adoptopenjdk.jitwatch.core.JITWatchConstants.S_CLOSE_BRACE;

import java.util.ArrayList;
import java.util.List;

import org.adoptopenjdk.jitwatch.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BytecodeInstruction
{
	private int offset;
	private Opcode opcode;

	private List<IBytecodeParam> parameters = new ArrayList<>();

	private boolean hasComment;
	private String comment;

	private static final Logger logger = LoggerFactory.getLogger(BytecodeInstruction.class);

	public int getOffset()
	{
		return offset;
	}

	public void setOffset(int offset)
	{
		this.offset = offset;
	}

	public Opcode getOpcode()
	{
		return opcode;
	}

	public void setOpcode(Opcode opcode)
	{
		this.opcode = opcode;
	}

	public List<IBytecodeParam> getParameters()
	{
		return parameters;
	}

	public void addParameter(IBytecodeParam parameter)
	{
		this.parameters.add(parameter);
	}

	public boolean hasParameters()
	{
		return parameters.size() > 0;
	}

	public String getComment()
	{
		return comment;
	}

	public String getCommentWithMemberPrefixStripped()
	{
		if (comment != null && comment.startsWith(S_BYTECODE_METHOD_COMMENT))
		{
			return comment.substring(S_BYTECODE_METHOD_COMMENT.length()).trim();
		}
		else if (comment != null && comment.startsWith(S_BYTECODE_INTERFACEMETHOD_COMMENT))
		{
			return comment.substring(S_BYTECODE_INTERFACEMETHOD_COMMENT.length()).trim();
		}
		else
		{
			return comment;
		}
	}

	public void setComment(String comment)
	{
		this.comment = comment;
		hasComment = true;
	}

	public boolean hasComment()
	{
		return hasComment;
	}

	@Override
	public String toString()
	{
		return toString(0, 0);
	}

	public boolean isInvoke()
	{
		return opcode != null
				&& (opcode == Opcode.INVOKEVIRTUAL || opcode == Opcode.INVOKESPECIAL || opcode == Opcode.INVOKESTATIC
				|| opcode == Opcode.INVOKEINTERFACE || opcode == Opcode.INVOKEDYNAMIC);

	}

	public boolean isSwitch()
	{
		return opcode != null && (opcode == Opcode.TABLESWITCH || opcode == Opcode.LOOKUPSWITCH);
	}

	public int getLabelLines()
	{
		int result = 1;

		if (isSwitch() && parameters.size() == 1)
		{
			BCParamSwitch switchParam = (BCParamSwitch) parameters.get(0);
			result = 2 + switchParam.getSize();
		}

		return result;
	}

	public String toString(int maxOffset, int line)
	{
		if (isSwitch())
		{
			return toStringSwitch(maxOffset, line);
		}
		else
		{
			return toStringNonSwitch(maxOffset);
		}
	}

	private String toStringNonSwitch(int maxOffset)
	{
		StringBuilder toStringBuilder = new StringBuilder();

		int offsetWidth = Integer.toString(maxOffset).length();

		toStringBuilder.append(StringUtil.alignRight(offset, offsetWidth)).append(C_COLON).append(C_SPACE);

		if (opcode != null)
		{
			toStringBuilder.append(StringUtil.alignLeft(opcode.getMnemonic(), 16));
		}

		if (hasParameters())
		{
			StringBuilder paramBuilder = new StringBuilder();

			for (IBytecodeParam parameter : parameters)
			{
				paramBuilder.append(parameter.toString()).append(C_COMMA).append(C_SPACE);
			}

			int paramLength = paramBuilder.length();

			paramBuilder.delete(paramLength - 2, paramLength);

			String paramString = StringUtil.alignLeft(paramBuilder.toString(), 5);

			toStringBuilder.append(paramString);
		}

		if (hasComment)
		{
			toStringBuilder.append(comment);
		}

		return toStringBuilder.toString();
	}

	private String toStringSwitch(int maxOffset, int line)
	{
		int maxLines = getLabelLines();

		StringBuilder toStringBuilder = new StringBuilder();

		int offsetWidth = Integer.toString(maxOffset).length();

		if (line == 0)
		{

			toStringBuilder.append(StringUtil.alignRight(offset, offsetWidth)).append(C_COLON).append(C_SPACE);
			toStringBuilder.append(opcode.getMnemonic());

			toStringBuilder.append(C_SPACE).append(C_OPEN_BRACE);

			if (hasComment)
			{
				toStringBuilder.append(C_SPACE).append(comment);
			}
		}
		else if (line == maxLines - 1)
		{
			toStringBuilder.append(StringUtil.alignRight(S_CLOSE_BRACE, offsetWidth + 3));
		}
		else
		{
			if (parameters.size() == 1 && parameters.get(0) instanceof BCParamSwitch)
			{
				BCParamSwitch param = (BCParamSwitch) parameters.get(0);

				toStringBuilder.append(param.toString(line - 1));
			}
			else
			{
				logger.error("Bad parameters set on tableswitch or lookupswitch, cannot create toString()");
			}
		}

		return toStringBuilder.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comment == null) ? 0 : comment.hashCode());
		result = prime * result + (hasComment ? 1231 : 1237);
		result = prime * result + offset;
		result = prime * result + ((opcode == null) ? 0 : opcode.hashCode());
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (obj == null)
		{
			return false;
		}

		if (getClass() != obj.getClass())
		{
			return false;
		}

		BytecodeInstruction other = (BytecodeInstruction) obj;

		if (comment == null)
		{
			if (other.comment != null)
			{
				return false;
			}
		}
		else if (!comment.equals(other.comment))
		{
			return false;
		}

		if (hasComment != other.hasComment)
		{
			return false;
		}

		if (offset != other.offset)
		{
			return false;
		}

		if (opcode != other.opcode)
		{
			return false;
		}

		if (parameters == null)
		{
			if (other.parameters != null)
			{
				return false;
			}
		}
		else if (!parameters.equals(other.parameters))
		{
			return false;
		}

		return true;
	}
}
