package com.webcodepro.applecommander.ui;

/*
 * Copyright (C) 2012 by David Schmidt
 * david__schmidt at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 2 of the License, or (at your 
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.FormattedDisk;

public class AntTask extends Task
{
	public void execute() throws BuildException
	{
		/*
		 * Commands: 
		 * cc65: <imagename> <filename> <type> 
		 * d: <imagename> <filename>
		 * e: <imagename> <filename>
		 * i: <imagename>
		 * k/u: <imagename> <filename>
		 * ls/l/ll: <imagename>
		 * n: <imagename> <volname> 
		 * p: <imagename> <filename> <type> [<address>]
		 * x: <imagename> <outputpath>
		 * dos140: <imagename> <volname>
		 * pro140/pro800: <imagename> <volname>
		 * pas140/pas800: <imagename> <volname>
		 * convert: <filename> <imagename> [<sizeblocks>]
		 */
		if (_command.equals("i"))
		{
			try
			{
				String[] onlyOneImage = { "nonsense", _imageName };
				com.webcodepro.applecommander.ui.ac.getDiskInfo(onlyOneImage);
			}
			catch (Exception ex)
			{
				if (_failonerror)
					throw new BuildException(ex);
				else
					System.out.println(ex.getMessage());
			}
		}
		else if (_command.equals("e") || _command.equals("g"))
		{
			PrintStream outfile = System.out;
			try
			{
				if (_output != null)
				{
					outfile = new PrintStream(new FileOutputStream(_output));
				}
				com.webcodepro.applecommander.ui.ac.getFile(_imageName, _fileName, _command.equals("e"), outfile);
			}
			catch (Exception ex)
			{
				if (_failonerror)
					throw new BuildException(ex);
				else
					System.out.println(ex.getMessage());
			}
		}
		else if (_command.equals("p") || (_command.equals("cc65")))
		{
			try
			{
				if (_command.equals("p"))
				{
					com.webcodepro.applecommander.ui.ac.putFile(_input, _imageName, _fileName, _type, _address);
				}
				else
					com.webcodepro.applecommander.ui.ac.putCC65(_input, _imageName, _fileName, _type);
			}
			catch (Exception ex)
			{
				if (_failonerror)
					throw new BuildException(ex);
				else
					System.out.println(ex.getMessage());
			}
		}
		else if (_command.equals("d"))
		{
			try
			{
				com.webcodepro.applecommander.ui.ac.deleteFile(_imageName, _fileName);
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else if (_command.equals("n"))
		{
			try
			{
				com.webcodepro.applecommander.ui.ac.setDiskName(_imageName, _volName);
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else if (_command.equals("k") || _command.equals("u"))
		{
			try
			{
				if (_command.equals("k"))
					com.webcodepro.applecommander.ui.ac.setFileLocked(_imageName, _fileName, true);
				else // Assume unlock
					com.webcodepro.applecommander.ui.ac.setFileLocked(_imageName, _fileName, false);
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else if (_command.equals("ls") || _command.equals("l") || _command.equals("ll"))
		{
			try
			{
				String[] onlyOneImage = { "nonsense", _imageName };
				if (_command.equals("ls"))
					com.webcodepro.applecommander.ui.ac.showDirectory(onlyOneImage, FormattedDisk.FILE_DISPLAY_STANDARD);
				else if (_command.equals("l"))
					com.webcodepro.applecommander.ui.ac.showDirectory(onlyOneImage, FormattedDisk.FILE_DISPLAY_NATIVE);
				else // Assume "ll"
					com.webcodepro.applecommander.ui.ac.showDirectory(onlyOneImage, FormattedDisk.FILE_DISPLAY_DETAIL);
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else if (_command.equals("dos140"))
		{
			try
			{
				com.webcodepro.applecommander.ui.ac.createDosDisk(_imageName, Disk.APPLE_140KB_DISK);
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else if ((_command.equals("pro800") || _command.equals("pro140")))
		{
			try
			{
				if (_command.equals("pro800"))
					com.webcodepro.applecommander.ui.ac.createProDisk(_imageName, _volName, Disk.APPLE_800KB_DISK);
				else
					com.webcodepro.applecommander.ui.ac.createProDisk(_imageName, _volName, Disk.APPLE_140KB_DISK);
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else if ((_command.equals("pas800") || _command.equals("pas140")))
		{
			try
			{
				if (_command.equals("pas800"))
					com.webcodepro.applecommander.ui.ac.createPasDisk(_imageName, _volName, Disk.APPLE_800KB_DISK);
				else
					com.webcodepro.applecommander.ui.ac.createPasDisk(_imageName, _volName, Disk.APPLE_140KB_DISK);
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else if (_command.equals("x"))
		{
			try
			{
				com.webcodepro.applecommander.ui.ac.getFiles(_imageName, _outputPath);
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else if (_command.equals("convert"))
		{
			try
			{
				com.webcodepro.applecommander.ui.ac.convert(_fileName, _imageName, Integer.parseInt(_sizeBlocks));
			}
			catch (IOException io)
			{
				if (_failonerror)
					throw new BuildException(io);
				else
					System.out.println(io.getMessage());
			}
		}
		else
		{
			throw new BuildException("Command \"" + _command + "\" not implemented.");
		}
	}

	public void setCommand(String command)
	{
		_command = command;
	}

	public void setInput(String input)
	{
		_input = input;
	}

	public void setOutput(String output)
	{
		_output = output;
	}

	public void setImageName(String imageName)
	{
		_imageName = imageName;
	}

	public void setFileName(String fileName)
	{
		_fileName = fileName;
	}

	public void setOutputPath(String outputPath)
	{
		_outputPath = outputPath;
	}

	public void setVolName(String volName)
	{
		_volName = volName;
	}

	public void setType(String type)
	{
		_type = type;
	}

	public void setAddress(String address)
	{
		_address = address;
	}

	public void setSizeBlocks(String sizeBlocks)
	{
		_sizeBlocks = sizeBlocks;
	}

	public void setFailOnError(String failonerror)
	{
		if (failonerror.equalsIgnoreCase("true"))
			_failonerror = true;
		if (failonerror.equalsIgnoreCase("false"))
			_failonerror = false;
	}

	boolean _failonerror = true;

	String _input = null;

	String _output = null;

	String _command = null;

	String _imageName = null;

	String _fileName = null;

	String _volName = "ACDISK";

	String _outputPath = null;

	String _type = null;

	String _address = "0x2000";

	String _sizeBlocks = "0";
}
