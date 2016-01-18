#!bin/sh


# Make a Postscript/PDF listing of the Nachos code, suitable for duplication

COPYRIGHT=COPYRIGHT

README=README.stoller README.stark README.druschel test/README

MAKEFILES=Makefile test/Makefile

KERNEL=\
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
	nachos/util/Queue.java\
	
MACHINE=\
	nachos/machine/MachineException.java\
	nachos/machine/Simulation.java\
	nachos/machine/MalformedPacketException.java\
	nachos/machine/SegmentDescriptor.java\
	nachos/machine/SerialPort.java\
	nachos/machine/Network.java\
	nachos/machine/TranslationEntry.java\
	nachos/machine/RK05Disk.java\
	nachos/machine/Timer.java\
	nachos/machine/CPU.java\
	nachos/machine/InterruptHandler.java\
	nachos/machine/GUIConsole.java\
	nachos/machine/MIPS.java\
	nachos/machine/TestAndSetVariable.java\
	nachos/machine/Disk.java\
	nachos/machine/NachosThread.java\
	nachos/machine/MMU.java\
	nachos/machine/Console.java\
	nachos/machine/StreamConsole.java\
	nachos/machine/ExceptionHandler.java\
	nachos/machine/PendingInterrupt.java\
	nachos/machine/Machine.java\
	nachos/machine/Statistics.java\
	nachos/machine/Packet.java\

NOFF=\
	nachos/noff/NoffOutput.java\
	nachos/noff/Coff2Noff.java\
	nachos/noff/CoffInput.java\
	nachos/noff/NoffHeader.java

TEST=   test/copyright.h\
	test/syscall.h\
	test/halt.c\
	test/matmult.c\
	test/shell.c\
	test/sort.c\
	test/start.s\
	test/script

all: nachos.pdf

nachos.pdf: nachos.ps
	ps2pdf nachos.ps

nachos.ps:
	cd ..; \
	a2ps -o - -b --margin=24 --pretty-print --print-anyway=1 \
		${COPYRIGHT} ${README} ${MAKEFILES} \
		${KERNEL} ${TEST} > print/nachos.ps

clean:
	rm -f *~ \#* *.ps *.pdf
