\NeedsTeXFormat{LaTeX2e}[1995/06/01]
\ProvidesClass{uiophd}[2011/06/22 v 1.02 UiO document class]
\DeclareOption{10pt}{\ClassWarningNoLine{uiophd}{Do not
        use type size `10pt'}}
\DeclareOption{11pt}{\ClassWarningNoLine{uiophd}{Do not
        use type size `11pt'}}
\DeclareOption{12pt}{}
\DeclareOption{american}{\renewcommand{\uiophd@lang}{american}}
\DeclareOption{english}{\renewcommand{\uiophd@lang}{english}}
\newcommand{\uiophd@lang}{american}
\DeclareOption{altfont}{}
\DeclareOption*{\PassOptionsToClass{\CurrentOption}{report}}
\ProcessOptions \relax
\RequirePackage[\uiophd@lang]{babel}
\RequirePackage{emptypage}
\RequirePackage[T1]{fontenc}
\RequirePackage[a4paper]{geometry}
\RequirePackage{setspace}
\AtBeginDocument{\setstretch{1.1}}
\RequirePackage{textcomp}
\LoadClass[twoside,openright,12pt]{report}
\geometry{twoside,margin=2.5cm}
\IfFileExists{t1padx.fd}
  {\renewcommand{\rmdefault}{padx}}
  {\renewcommand{\rmdefault}{ptm}}
\IfFileExists{t1iofs.fd}
  {\renewcommand{\sfdefault}{iofs}}
  {\renewcommand{\sfdefault}{phv}}
\IfFileExists{lbtr.pfa}
  {\def \DeclareLucidaFontShape#1#2#3#4#5#6{%
  \DeclareFontShape{#1}{#2}{#3}{#4}{%
    <-5.5>s*[.96]#5%
    <5.5-6.5>s*[.92]#5%
    <6.5-7.5>s*[.88]#5%
    <7.5-8.5>s*[.84]#5%
    <8.5-9.5>s*[.82]#5%
    <9.5-10.5>s*[.8]#5%
    <10.5-11.5>s*[.78]#5%
    <11.5-13>s*[.76]#5%
    <13-15.5>s*[.74]#5%
    <15.5-18.5>s*[.72]#5%
    <18.5-22.5>s*[.70]#5%
    <22.5->s*[.68]#5%
  }{#6}}
    \renewcommand{\ttdefault}{hlct}}
  {\renewcommand{\ttdefault}{pcr}}
\newcommand{\frontmatter}{\cleardoublepage
  \pagenumbering{roman}}
\newcommand{\mainmatter}{\cleardoublepage
  \pagenumbering{arabic}}
\newcommand{\backmatter}{\cleardoublepage}
