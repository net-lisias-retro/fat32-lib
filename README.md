fat32-lib-LST
==============

This library allows to manipulate FAT file systems using the Java programming language.
Because of it's age and simplicity, FAT can be called the least common denominator in
file systems, being used in digital cameras, cell phones, ... and being supported by
almost every operating system in existence. This project aims for making FAT file
systems accessible for Java programs without using the operating system to interpret
the on-disk structures. Instead, we provide a pure - Java implementation of the FAT
specification from MICROS~1. 

This fork aims to allow easy support to different Computer Systems that use FAT or some similar file system.


Features
--------

The following features are currently supported:

  * creating FAT12, FAT16 and FAT32 file systems through the super floppy formatter
  * r/w access to FAT12, FAT16 and FAT32 file systems
  * manipulating the FAT file attributes (archive, hidden, system and read-only)
  * r/w access to the FAT's volume label
  * no external dependencies
  * easily extendable


Getting started
---------------

(this section needs update. WiP)

To use the fat32-lib you will have to add it to the classpath of your project. For Maven
users it is sufficient to add

~~~~
<dependency>
    <groupId>de.waldheinz</groupId>
    <artifactId>fat32-lib</artifactId>
    <version>0.6.5</version>
</dependency>
~~~~

to the dependencies section of your pom. Now check out the [API docs](http://waldheinz.github.io/fat32-lib/apidocs/). If you're into creating FAT file system, the [SuperFloppyFormatter](http://waldheinz.github.io/fat32-lib/apidocs/de/waldheinz/fs/fat/SuperFloppyFormatter.html) would be a good starting point. And don't hestitate to ask if there are any questions.


History
-------

This library was originally based on the FAT file system driver included in the JNode operating
system. Since then, many bugs were fixed, the code was re-factored several times, and now I think
it is fair to call the fat32-lib an original implementation of the FAT file system family.

Then it was forked and heavily refactored to allow easy implementation of different filesystems from different computer Systems (as long as they are similar to FAT)


UPSTREAM
--------

https://github.com/waldheinz/fat32-lib
