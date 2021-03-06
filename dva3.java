var template = `
<script src="https://d3js.org/d3.v5.min.js"></script>
<script src="https://unpkg.com/topojson@3"></script>
<style>
body { 
    background-color: white;
}
.title { 
    font-size: 30px;
    font-family: "Roboto", sans-serif;
    text-align: center;
}
.container {
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
}
.directions {
    font-size: 18px;
    font-family: "Roboto", sans-serif;
    padding-left: 20px;
}
#backgroundRectangle {
    width: 100%;
    height: 100%;
    fill: #f5f5f5;
}
.projectionOutline{
    fill: #2f434a;
    stroke: #4e5f66
}
.tooltip {
    position: absolute;
    font-size: 12px;
    width: auto;
    height: auto;
    pointer-events: none;
    background-color: white;
    padding: 3px;
}

</style>
<h1 class="title"></h1>
<div class="container">
<div id="map"></div>
<p class="directions">Draw mouse to zoom in onto section. Double click to zoom out</p>
</div>
<script>

    pm.getData( function(err, value) {
        d3.select(".title").html(value.title);
        initVisualization(value.data);
    });

    // Function call that contains our visualization, necessary because we are loading an external map file
    async function initVisualization(pmInput){
        const response = await fetch("https://unpkg.com/world-atlas@1.1.4/world/110m.json");
        response.json().then( data => {
            generateVisualization(pmInput, data);
        });
    }
    
    // Generates d3 map visualization using an external map file and user-inputed data
    // Utilizes d3.zoom and d3.brush
    function generateVisualization(pmInput, mapData){
        // Set the dimensions and margins of the graph
        const margins = {top: 20, left: 10, right: 20, bottom: 20};
        const width = 800 - margins.top - margins.bottom;
        const height = 500 - margins.left - margins.right;
        
        // Initializes d3.zoom to cover entire map SVG
        const zoom = d3.zoom()
            .scaleExtent([1,40])
            .translateExtent([[0,0],[width + margins.left + margins.right, height + margins.top + margins.bottom]])
            .extent([[0,0],[width + margins.left + margins.right, height + margins.top + margins.bottom]])
            .on("zoom", () =>{
                d3.select("#map-group").attr("transform", d3.event.transform)
            })
        
        // Initiaizes d3.brush to cover entire map SVG and zoom in on the selected window
        let brush = d3.brush()
            .extent([[0,0],[width + margins.left + margins.right, height + margins.top + margins.bottom]])
            .on("end", () => {
                let extent = d3.event.selection;
                if(extent){
                    d3.select("#map-group").call(brush.move, null);
                    d3.select("#map-group").transition().duration(1500).call(zoom.transform, d3.zoomIdentity
                        .scale( (width + margins.left + margins.right)/ (extent[1][0]-extent[0][0]) )
                        .translate( -extent[0][0], -extent[0][1] ));
                    d3.selectAll("circle").transition().delay(750).duration(1000)
                        .attr("r", d => { return 2 * d.circleSize * (extent[1][0]-extent[0][0])/(width + margins.left + margins.right);} )
                        .attr("stroke-width", (extent[1][0]-extent[0][0])/(width + margins.left + margins.right));
                    
                }
                else{
                    d3.select("#map-group").transition().duration(1500).call(zoom.transform, d3.zoomIdentity
                        .scale( 1 )
                        .translate( 0,0 ));
                    d3.selectAll("circle").transition().delay(750).duration(1000)
                        .attr("r", d => d.circleSize)
                        .attr("stroke-width", 1);
                }
            });
        
        // Set the dimensions and margins of the graph
        let svg = d3.select("#map")
          .append("svg")  
            .attr("width", width + margins.left + margins.right)
            .attr("height", height + margins.top + margins.bottom)
        svg.append("rect")
            .attr("id", "backgroundRectangle");
        svg = svg.append("g")
            .attr("id", "map-group")
            .call(brush);
        
        // Draws Mercator projection of map onto SVG using the inputted map file
        let projection = d3.geoMercator().translate([400, 350]).scale(125);
        var mapGroup = svg.append("g");
        let mapPath = d3.geoPath().projection(projection);
        mapGroup.selectAll("path")
            .data(topojson.feature(mapData, mapData.objects.countries).features)
            .enter()
          .append("path")
            .attr("d", mapPath)
            .attr("class", "projectionOutline");
        
        // Calculate offset for tooltip
        const rect = document.getElementById("map").getBoundingClientRect();
        const offset = {top: rect.top, left: rect.left};
        
        // Create hover tooltip
        let tooltip = d3.select("#map").append("div")
            .attr("class", "tooltip");
        // tooltip mouseover event handler
        let tipMouseover = function(d){
            tooltip.html("Longitude: <b>" + d.long + "</b><br/>Latitude: <b>" + d.lat + "</b>")
                .style("left", (d3.event.pageX + 15 - offset.left) + "px")
                .style("top", (d3.event.pageY - 20 - offset.top) + "px")
              .transition()
                .duration(200)      // ms
                .style("opacity", 0.9)
            d3.select(this)
                .style("stroke", "white")
                .style("opacity", 1);
        };
        // tooltip mouseout event handler
        let tipMouseout = function(d){
            tooltip.transition()
                .duration(300)
                .style("opacity", 0);
            d3.select(this)
                .style("stroke", "none")
                .style("opacity", 0.6);
        };
        
        // Appends points from user-inputted data onto map
        svg.selectAll("circle")
            .data(pmInput)
            .enter()
          .append("circle")
            .attr("r", 0)
            .style("fill", d => d.color)
            .attr("cx", d => projection([d.long, d.lat])[0])
            .attr("cy",  d => projection([d.long, d.lat])[1])
            .style("opacity", 0.6)
            .on("mouseover", tipMouseover)
            .on("mouseout", tipMouseout)
          .transition(d3.transition().duration(1000).ease(d3.easeQuadOut))
            .attr("r", d => d.circleSize)
    }
</script>
`;
var response = pm.response.json();
let parsedData = [];
//data parsing
for (let data of response) {
    let tempEntry = {};
    tempEntry.lat = data.coordinates.latitude;
    tempEntry.long = data.coordinates.longitude;
    tempEntry.circleSize = Math.abs(Math.ceil(data.stats.confirmed / 8000));
    tempEntry.color = "#F8DBEF";
    tempEntry.date = parsedData.updatedAt;
    parsedData.push(tempEntry);
}
pm.visualizer.set(template, {
    data: parsedData,
    title: "Map of COVID-19 confirmed cases daily"
});