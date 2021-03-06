<#include "/templates/system/common/cstudio-support.ftl" />
<#include "/templates/web/navigation/navigation.ftl">
<!DOCTYPE html>
<html lang="en">
<head>

    <meta charset="utf-8">
    <title>Rosie Rivet - Crafter Rivet Demo Site</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="Rivet Logic Corporation">

        <link href="/static-assets/css/main.css" rel="stylesheet">

    <!--[if lt IE 9]>
    <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

</head>
<body>
AAAAAAAAAAAAAA
<div id="main-container" class="promo-page">
            	
<#include "/templates/web/fragments/header.ftl"/>

<div class="container-fluid" id="content-body">
    
    <div class="row-fluid">
        <div class="span3 mb10" id="site-nav">
        
        	<div class="input-append" id="site-search">
	        	<input type="text" class="wauto" placeholder="search" />
	        	<a class="add-on">
		        	<i class="icon icon-search"></i>
	        	</a>
        	</div>
            
			<ul class="nav nav-list amaranth uppercase">
				<@renderNavigation "/site/website", 1 />
			</ul>
            
        </div>
        <div class="span9" id="content">
            
            <div class="big-banner relative">
            
            	<img src="/static-assets/images/promo-banner.png" 
            		 alt="fill in accurate image description here..." 
            		 title="fill in accurate image description here..." />
            		 
            	<p class="promo-desc tac amaranth mt20">
            		<span class="big">Whats New Now</span><br/>
            		shop new arrivals and<br/>
            		take an extra 15 % off<br/>
            		for a limited time<br/>
            	</p>
            
            	<button class="btn btn-danger uppercase left">
            		Shop Mens
            	</button>
            
            	<button class="btn btn-danger uppercase right">
            		Shop Womens
            	</button>
            
            </div>
            
        </div>
    </div>

    <hr>

<#include "/templates/web/fragments/footer.ftl"/>

</div>
<!-- /container -->

</div>

<script src="/static-assets/js/jquery.min.js"></script>
<script src="/static-assets/js/bootstrap.min.js"></script>
<script src="/static-assets/js/main.js"></script>
<@cstudioOverlaySupport/>

</body>
</html>
