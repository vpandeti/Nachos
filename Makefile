#
# Makefile for NACHOS.
# Not very compact, but it should be compatible with pretty much any version
# of "make".  Using "make" should help avoid strange situations where "javac"
# fails to compile things that it probably ought to.
#
# When you add a source file to NACHOS, also add additional lines below,
# following the existing pattern.  Note that many of the lines below start
# with a TAB character, not spaces, and this is essential for correct
# interpretation by "make".
#

# Uncomment the following when running on Windows.
JC= javac -classpath 'machine.jar;.'

# Uncomment the following when running on Unix.
#JC= javac -classpath 'machine.jar:.'

KERNEL_SOURCES=\
	nachos/Debug.java\
	nachos/Options.java\
	nachos/kernel/Nachos.java\
	nachos/kernel/devices/test/SerialTest.java\
	nachos/kernel/devices/test/ConsoleTest.java\
	nachos/kernel/devices/test/NetworkTest.java\
	nachos/kernel/devices/ConsoleDriver.java\
	nachos/kernel/devices/SerialDriver.java\
	nachos/kernel/devices/NetworkDriver.java\
	nachos/kernel/devices/DiskDriver.java\
	nachos/kernel/filesys/test/FileSystemTest.java\
	nachos/kernel/filesys/OpenFileReal.java\
	nachos/kernel/filesys/Directory.java\
	nachos/kernel/filesys/FileSystemStub.java\
	nachos/kernel/filesys/DirectoryEntry.java\
	nachos/kernel/filesys/OpenFileStub.java\
	nachos/kernel/filesys/FileHeader.java\
	nachos/kernel/filesys/OpenFile.java\
	nachos/kernel/filesys/FileSystem.java\
	nachos/kernel/filesys/BitMap.java\
	nachos/kernel/filesys/FileSystemReal.java\
	nachos/kernel/threads/test/SMPTest.java\
	nachos/kernel/threads/test/ThreadTest.java\
	nachos/kernel/threads/Condition.java\
	nachos/kernel/threads/Lock.java\
	nachos/kernel/threads/Scheduler.java\
	nachos/kernel/threads/SpinLock.java\
	nachos/kernel/threads/Semaphore.java\
	nachos/kernel/userprog/test/ProgTest.java\
	nachos/kernel/userprog/UserThread.java\
	nachos/kernel/userprog/Syscall.java\
	nachos/kernel/userprog/AddrSpace.java\
	nachos/kernel/userprog/ExceptionHandler.java\
	nachos/util/FIFOQueue.java\
	nachos/util/Queue.java

MACHINE= machine.jar

default: kernel

kernel: $(KERNEL_SOURCES)
	${JC} $(KERNEL_SOURCES)

javadoc:
	./makejavadoc

cleanclass:
	rm -f *.class */*.class */*/*.class */*/*/*.class */*/*/*/*.class 

cleanbackup:
	rm -f *~ */*~ */*/*~ */*/*/*~ */*/*/*/*~

# The following definitions are more general, but they work on Windows
# only if the directory containing cygwin's find.exe precedes the directory
# containing Windows's find.exe in PATH.
#cleanclass:
#	(find . -name "*.class" -print | xargs rm -f)
#
#cleanbackup:
#	(find . -name "*~" -print | xargs rm -f)

# The machine.jar file is not to be rebuilt by students.
# The Cygwin "make" seems to insist on the Java source files existing
# even when machine.jar is present, so leave this commented out.
#
#$(MACHINE): nachos/machine/*.java nachos/noff/*.java javadoc
#	${JC} nachos/machine/*.java nachos/noff/*.java
#	jar cvf machine.jar nachos/machine/*.class nachos/noff/*.class \
#		doc/machine

